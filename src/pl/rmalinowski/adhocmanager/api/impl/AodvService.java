package pl.rmalinowski.adhocmanager.api.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.rmalinowski.adhocmanager.api.NetworkLayerService;
import pl.rmalinowski.adhocmanager.api.PhysicalLayerService;
import pl.rmalinowski.adhocmanager.events.NetworkLayerEvent;
import pl.rmalinowski.adhocmanager.events.NetworkLayerEventType;
import pl.rmalinowski.adhocmanager.events.PhysicalLayerEvent;
import pl.rmalinowski.adhocmanager.model.ErrorNode;
import pl.rmalinowski.adhocmanager.model.Node;
import pl.rmalinowski.adhocmanager.model.RoutingTableEntry;
import pl.rmalinowski.adhocmanager.model.RoutingTableEntryState;
import pl.rmalinowski.adhocmanager.model.packets.DataPacket;
import pl.rmalinowski.adhocmanager.model.packets.Packet;
import pl.rmalinowski.adhocmanager.model.packets.RERRMessage;
import pl.rmalinowski.adhocmanager.model.packets.RREPMessage;
import pl.rmalinowski.adhocmanager.model.packets.RREQMessage;
import pl.rmalinowski.adhocmanager.model.packets.RoutingPacket;
import pl.rmalinowski.adhocmanager.persistence.NodeDao;
import pl.rmalinowski.adhocmanager.utils.AhHocManagerConfiguration;
import pl.rmalinowski.adhocmanager.utils.AodvContants;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class AodvService extends NetworkLayerService {
	private static final String TAG = "AodvService";

	private final IBinder mBinder = new MyBinder();

	private PhysicalLayerService physicalService;
	private Set<RoutingTableEntry> routingTable;
	private volatile Map<String, LinkedList<DataPacket>> dataPacketsQueues;
	private NodeDao nodeDao;
	private volatile Integer nodeSequenceNumber;
	private volatile Integer rreqMessageId;
	private Map<String, Long> recentlyRecievedRreqMessagesTimestamps;
	private CheckRoutingTableThread checkRoutingTableThread;

	@Override
	public void onCreate() {
		registerBroadcastRecievers();
		nodeDao = new NodeDao(this);
		nodeDao.open();
		initializeNetworkLayer();
		bindService(new Intent(this, AhHocManagerConfiguration.physicalLayerClass), mConnection, Context.BIND_AUTO_CREATE);
		super.onCreate();
	}

	private void initializeNetworkLayer() {
		nodeSequenceNumber = 1;
		rreqMessageId = 1;
		dataPacketsQueues = new HashMap<String, LinkedList<DataPacket>>();
		recentlyRecievedRreqMessagesTimestamps = new HashMap<String, Long>();
		initializeRoutingTable();
		checkRoutingTableThread = new CheckRoutingTableThread(AhHocManagerConfiguration.ROUTING_TABLE_REFRESH_INTERVAL);
		checkRoutingTableThread.start();
	}

	private void initializeRoutingTable() {
		routingTable = new HashSet<RoutingTableEntry>();
		Set<Node> nodes = nodeDao.getAllNodes();
		for (Node node : nodes) {
			RoutingTableEntry entry = new RoutingTableEntry(node);
			routingTable.add(entry);
		}
	}

	@Override
	public void sendData(Serializable data, String destinationAddress) {
		RoutingTableEntry entry = findRoutingTableEntryForAddress(destinationAddress);
		RoutingTableEntryState entryState = checkNodeValidity(entry);

		DataPacket packet = new DataPacket(data);
		packet.setDestinationAddress(destinationAddress);
		packet.setSourceAddress(physicalService.getLocalAddress());
		packet.setHopCount(1);
		String nextHopAddress = entry.getNextHopAddress();
		switch (entryState) {
		// jesli wpis o wezle docelowym jest aktualny
		case VALID:

			// zaktualizuj dane o wezle docelowym
			makeRoutingTableEntryValidLonger(destinationAddress);
			// jesli nie jest sasiadem, zaktualizuj takze informacje o nastepnym
			// skoku
			if (!nodeIsNeighbour(entry)) {
				makeRoutingTableEntryValidLonger(nextHopAddress);
			}

			// wyslij wiadomosc do warstwy fizycznej
			physicalService.sendPacket(packet, nextHopAddress);
			break;
		// jesli jest nieaktualny
		case INVALID:
			// zdeaktualizuj wezel
			entry.setState(entryState);
			// rozpocznij watek wysylajacy wiadomosci RREQ
			RreqMessageSenderThread rreqMessageSender = new RreqMessageSenderThread(entry);
			rreqMessageSender.start();
			// dodaj pakiety do kolejnki oczekujacej na wyslanie
			addDataPacketToWaitingQueue(packet);
			// ustaw status wezla na walidowany
			entry.setState(RoutingTableEntryState.VALIDATING);
			break;
		// jesli juz zostaly wyslane wiadomosci typu RREQ
		case VALIDATING:
			// dodaj pakiety do kolejnki oczekujacej na wyslanie
			addDataPacketToWaitingQueue(packet);
			break;
		default:
			break;
		}
		sendNetworkBroadcast(new NetworkLayerEvent(NetworkLayerEventType.NETWORK_STATE_CHANGED));
	}

	private void addDataPacketToWaitingQueue(DataPacket packet) {
		// jezeli kolejka juz istnieje dodaj element
		if (dataPacketsQueues.containsKey(packet.getDestinationAddress())) {
			dataPacketsQueues.get(packet.getDestinationAddress()).addLast(packet);
		} // jezeli kolejka nie istnieje, stworz ja i dodaj element
		else {
			LinkedList<DataPacket> list = new LinkedList<DataPacket>();
			list.addLast(packet);
			dataPacketsQueues.put(packet.getDestinationAddress(), list);
		}
	}

	private void handlePhysicalLayerEvent(PhysicalLayerEvent event) {
		switch (event.getEventType()) {
		case ADAPTER_NOT_ENABLED:
			// sendNetworkBroadcast(new
			// NetworkLayerEvent(NetworkLayerEventType.ADAPTED_DISABLED,
			// event.getData()));
			break;
		case CONNECTION_TO_NEIGHBOUR_ESTABLISHED:
			String address = (String) event.getData();
			RoutingTableEntry entry = findRoutingTableEntryForAddress(address);
			entry.setState(RoutingTableEntryState.VALID);
			entry.setHopCount(1);
			entry.setNextHopAddress(address);
			entry.setValidTimestamp(new Date().getTime() + AodvContants.NIEGHBOUR_ACTIVE_ROUTE_TIMEOUT);
			sendNetworkBroadcast(new NetworkLayerEvent(NetworkLayerEventType.SHOW_TOAST, "Podlaczono do sasiada"));
			break;
		case CONNECTING_TO_NEIGHBOURS_FINISHED:
			sendNetworkBroadcast(new NetworkLayerEvent(NetworkLayerEventType.SHOW_TOAST, "uruchomiono warstwe fizyczna"));
			break;
		case PACKET_RECEIVED:
			handleRecievedPacket((Packet) event.getData());
			break;
		case NEW_NODE_ADDED:
			Node node = (Node) event.getData();
			RoutingTableEntry newEntry = new RoutingTableEntry(node);
			routingTable.add(newEntry);
			sendNetworkBroadcast(new NetworkLayerEvent(NetworkLayerEventType.SHOW_TOAST, "dodano nowy wezel"));
			break;
		case CONNECTION_TO_NEIGHBOUR_LOST:
			String addressOfLostedNeighbour = (String) event.getData();
			handleNeighbourLost(addressOfLostedNeighbour);
			sendNetworkBroadcast(new NetworkLayerEvent(NetworkLayerEventType.SHOW_TOAST, "Utracono polaczenie do sasiada"));
			break;
		case CONNECTION_TO_NEIGHBOUR_NOT_ESTABLISHED:
			sendNetworkBroadcast(new NetworkLayerEvent(NetworkLayerEventType.SHOW_TOAST, "Nie udalo sie nawiazac polaczenia do sasiada"));
			break;
		default:
			break;
		}
	}

	private void handleRecievedPacket(Packet packet) {
		if (packet instanceof DataPacket) {
			handleRecievedDataPacket((DataPacket) packet);
		} else if (packet instanceof RoutingPacket) {
			handleRecievedRoutingPacket((RoutingPacket) packet);
		}
		sendNetworkBroadcast(new NetworkLayerEvent(NetworkLayerEventType.NETWORK_STATE_CHANGED));
	}

	private void handleRecievedDataPacket(DataPacket dataPacket) {
		// jezeli wezel jest adresatem wiadomosci
		if (physicalService.getLocalAddress().equals(dataPacket.getDestinationAddress())) {
			// wyslij informacje do wyzszych warstw
			sendNetworkBroadcast(new NetworkLayerEvent(NetworkLayerEventType.DATA_RECIEVED, dataPacket.getData()));
			// zaktualizuj dane o wezle ktory wyslal wiadomosc
			RoutingTableEntry sourceEntry = findRoutingTableEntryForAddress(dataPacket.getSourceAddress());
			makeRoutingTableEntryValid(sourceEntry);
			sourceEntry.setHopCount(dataPacket.getHopCount());
			sourceEntry.setNextHopAddress(dataPacket.getInterfaceAddress());
			// jezeli sasiad od ktorego otrzymano wiadomosc nie jest nadawca
			// zaktualizuj dane takze o nim
			if (!nodeIsNeighbour(dataPacket.getSourceAddress())) {
				makeRoutingTableEntryValid(dataPacket.getInterfaceAddress());
			}
		} // jezeli wezel nie jest adresatem wiadomosci
		else {
			RoutingTableEntry destinationEntry = findRoutingTableEntryForAddress(dataPacket.getDestinationAddress());
			// jezeli wpis w tabeli routingu do wezla docelowego jest aktualny
			if (RoutingTableEntryState.VALID == destinationEntry.getState()) {
				// podbij ilosc skokow dla wiadomosci
				dataPacket.setHopCount(dataPacket.getHopCount() + 1);
				// przekaz wiadomosc dalej do urzadzenia pobranego z tabeli
				// routingu
				physicalService.sendPacket(dataPacket, destinationEntry.getNextHopAddress());
				// nastepnie zaktualizuj informacje o wezle ktory wyslal
				// wiadomosc
				makeRoutingTableEntryValid(dataPacket.getSourceAddress());
				// jezeli sasiad od ktorego otrzymano wiadomosc nie jest nadawca
				// zaktualizuj dane takze o nim
				if (!nodeIsNeighbour(dataPacket.getSourceAddress())) {
					makeRoutingTableEntryValid(dataPacket.getInterfaceAddress());
				}
				// zaktualizuj takze informacje o adresacie
				makeRoutingTableEntryValid(dataPacket.getDestinationAddress());
				// jezeli wezel do ktorego przekazywana jest wiadomosc nie jest
				// adresatem, zaktualizuj takze dane o nim
				if (!nodeIsNeighbour(dataPacket.getDestinationAddress())) {
					makeRoutingTableEntryValid(destinationEntry.getNextHopAddress());
				}
			} // inaczej trzeba wyslac wiadomosc RERR
			else {
				makeRoutingTableEntryInvalid(destinationEntry);
				RERRMessage rerrMessage = new RERRMessage();
				ErrorNode errorNode = new ErrorNode();
				errorNode.setAddress(destinationEntry.getDestinationNode().getAddress());
				errorNode.setSequenceNumber(destinationEntry.getSequenceNumber());
				Set<ErrorNode> errorNodesSet = new HashSet<ErrorNode>();
				errorNodesSet.add(errorNode);
				rerrMessage.setUnreachableNodes(errorNodesSet);
				physicalService.sendPacket(rerrMessage, dataPacket.getInterfaceAddress());
			}
		}
	}

	private void handleRecievedRoutingPacket(RoutingPacket message) {
		if (message instanceof RERRMessage) {
			handleRERRMessage((RERRMessage) message);
		} else if (message instanceof RREQMessage) {
			handleRREQMessage((RREQMessage) message);
		} else if (message instanceof RREPMessage) {
			handleRREPMessage((RREPMessage) message);
		}
	}

	private void handleRERRMessage(RERRMessage message) {

		Set<ErrorNode> errorNodes = message.getUnreachableNodes();
		Set<RoutingTableEntry> nodesNotAccessibleAnymore = new HashSet<RoutingTableEntry>();
		for (ErrorNode errorNode : errorNodes) {
			RoutingTableEntry entry = findRoutingTableEntryForAddress(errorNode.getAddress());
			// jesli wpis z bledem byl dotad w tablicy routingu uznany za
			// prawidlowy, nalezy go zmienic na nieprawidlowy
			if (RoutingTableEntryState.VALID == entry.getState()) {
				entry.setState(RoutingTableEntryState.INVALID);
				entry.setSequenceNumber(errorNode.getSequenceNumber());
			}
			nodesNotAccessibleAnymore.add(entry);
		}
		determineAffectedNeighboursAndTransmitThemRerrMessage(nodesNotAccessibleAnymore);
	}

	private void handleNeighbourLost(String address) {
		Set<RoutingTableEntry> nodesNotAccessibleAnymore = findNodesWhichWasAccessedThroughNeighbour(address);
		determineAffectedNeighboursAndTransmitThemRerrMessage(nodesNotAccessibleAnymore);
	}

	private void determineAffectedNeighboursAndTransmitThemRerrMessage(Set<RoutingTableEntry> nodesNotAccessibleAnymore) {
		Set<String> affectedPrecursors = new HashSet<String>();
		for (RoutingTableEntry entry : nodesNotAccessibleAnymore) {
			affectedPrecursors.addAll(entry.getPrecursors());
		}
		Set<String> addressesOfNeighboursWhichHaveToBeInformed = findNextHopsWhichLeadsToNodesAffectedByBrokenLink(affectedPrecursors);

		RERRMessage rerrMessage = new RERRMessage();
		Set<ErrorNode> unreachableNodes = new HashSet<ErrorNode>();
		for (RoutingTableEntry entry : nodesNotAccessibleAnymore) {
			ErrorNode errorNode = new ErrorNode();
			errorNode.setAddress(entry.getDestinationNode().getAddress());
			// numer zostal juz wczesniej zwiekszony
			errorNode.setSequenceNumber(entry.getSequenceNumber());
			unreachableNodes.add(errorNode);
		}
		rerrMessage.setUnreachableNodes(unreachableNodes);

		for (String addressOfNode : addressesOfNeighboursWhichHaveToBeInformed) {
			physicalService.sendPacket(rerrMessage, addressOfNode);
		}
	}

	private Set<RoutingTableEntry> findNodesWhichWasAccessedThroughNeighbour(String address) {
		Set<RoutingTableEntry> affectedNodes = new HashSet<RoutingTableEntry>();
		for (RoutingTableEntry entry : routingTable) {
			if (address.equals(entry.getNextHopAddress())) {
				affectedNodes.add(entry);
				makeRoutingTableEntryInvalid(entry);
			}
		}
		// takze trzeba umiescic wezel ktory sie popsul
		affectedNodes.add(findRoutingTableEntryForAddress(address));
		return affectedNodes;
	}

	private Set<String> findNextHopsWhichLeadsToNodesAffectedByBrokenLink(Set<String> affectedNodes) {
		Set<String> resultSet = new HashSet<String>();

		for (String address : affectedNodes) {
			resultSet.add(findRoutingTableEntryForAddress(address).getNextHopAddress());
		}
		return resultSet;
	}

	private void handleRREQMessage(RREQMessage message) {

		// zaktualizuje informacje o wezle od ktorego dostales wiadomosc
		makeRoutingTableEntryValid(message.getInterfaceAddress());

		// sprawdz czy dostales juz RREQ o podanym id
		if (recentlyRecievedRreqMessagesTimestamps.containsKey(message.getId() + "_" + message.getOriginAddress())) {
			long lastRreqMessageTimestamp = recentlyRecievedRreqMessagesTimestamps.get(message.getId() + "_" + message.getOriginAddress());
			// jesli dana wiadomosc byla uzyskana niedawno to przerwij dalsze
			// wywolywanie metody
			if (new Date().getTime() < (lastRreqMessageTimestamp + AodvContants.PATH_DISCOVERY_TIME)) {
				return;
			}
		} // w innym przypadku dodaj do mapy odpowiedni wpis
		else {
			recentlyRecievedRreqMessagesTimestamps.put(message.getId() + "_" + message.getOriginAddress(), new Date().getTime());
		}
		// inkrementuj liczbe skokow
		message.setHopCount(message.getHopCount() + 1);

		// zaktualizuj sciezke do nadawcy
		RoutingTableEntry sourceEntry = findRoutingTableEntryForAddress(message.getOriginAddress());

		// jesli numer sekwencjny przeslany w wiadomosci jest wiekszy niz
		// ten w tablicy, nalezy zakutalizowac wpis w tablicy
		if (message.getOriginSeq() > sourceEntry.getSequenceNumber()) {
			sourceEntry.setSequenceNumber(message.getOriginSeq());
		}
		// zaktualizuj inne pola w tabeli rutingowej
		sourceEntry.setValidSequenceNumber(true);
		sourceEntry.setNextHopAddress(message.getInterfaceAddress());
		sourceEntry.setHopCount(message.getHopCount());
		makeRoutingTableEntryValid(sourceEntry);

		// jesli dany wezel jest wezlem docelowym to wyslij odpowiedz
		if (physicalService.getLocalAddress().equals(message.getDestinationAddress())) {
			// wygeneruj wiadomosc RREP
			RREPMessage rrepMessage = generateRrepMessageAsDestination(message);
			// odeslij pakiet RREP spowrotem droga ktora przyszedl
			physicalService.sendPacket(rrepMessage, message.getInterfaceAddress());
		} else {
			RoutingTableEntry destinationEntry = findRoutingTableEntryForAddress(message.getDestinationAddress());
			// aktualizujemy status wpisu w tablicy rutingowej dotyczacy wezla
			// docelowego
			RoutingTableEntryState destinationEntryState = checkNodeValidity(destinationEntry);
			// jesli wezel zna sciezke do wezla docelowego, jest aktualna i nie
			// jest ustawiona flaga D
			if (RoutingTableEntryState.VALID == destinationEntryState && destinationEntry.getSequenceNumber() >= message.getDestinationSeq()
					&& !message.getFlagD()) {
				// wygeneruj wiadomosc RREP
				RREPMessage rrepMessage = generateRrepMessageAsIntermediateNode(message);
				// odeslij pakiet RREP spowrotem droga ktora przyszedl
				physicalService.sendPacket(rrepMessage, message.getInterfaceAddress());
				// jesli w wiadomosci RREQ byla ustawiona flaga G, nalezy dalej
				// wyslac wiadomosc Gratuitous RREP
				if (message.getFlagG()) {
					RREPMessage gratuitousRrepMessage = generateGratuitousRREP(message);
					String gratuitousMessageDestinationAddress = findRoutingTableEntryForAddress(message.getDestinationAddress()).getNextHopAddress();
					physicalService.sendPacket(gratuitousRrepMessage, gratuitousMessageDestinationAddress);
				}
			} else {
				// jesli seqN w tablicy rutingowej jest wiekszy niz w
				// wiadomosci, nastepuje update w wiadomosci
				if (destinationEntry.getSequenceNumber() > message.getDestinationSeq()) {
					message.setDestinationSeq(destinationEntry.getSequenceNumber());
				}
				// przeslij broadcast dalej do wszystkich wezlow oprocz wezla od
				// ktorego dostales wiadomosc RREQ
				if (message.getTtl() > 1) {
					physicalService.sendPacketBroadcastExceptOneAddress(message, message.getInterfaceAddress());
				}
			}
		}
	}

	private void handleRREPMessage(RREPMessage message) {

		// zaktualizuje informacje o wezle od ktorego dostales wiadomosc
		makeRoutingTableEntryValid(message.getInterfaceAddress());

		message.setHopCount(message.getHopCount() + 1);
		RoutingTableEntry destinationEntry = findRoutingTableEntryForAddress(message.getDestinationAddress());
		// TODO koniecznie przetestowac
		// jezeli te warunki sa spelnione, nalezy zaktualizowac wpis dotyczacy
		// wezla ktorego dotyczy przekazywana dalej wiadomosc RREP
		if (!destinationEntry.getValidSequenceNumber()
				|| message.getDestinationSeq() > destinationEntry.getSequenceNumber()
				|| (message.getDestinationSeq().equals(destinationEntry.getSequenceNumber()) && (!destinationEntry.getState().equals(
						RoutingTableEntryState.VALID) || (destinationEntry.getHopCount() != null && message.getHopCount() < destinationEntry.getHopCount())))) {
			makeRoutingTableEntryValid(destinationEntry);
			destinationEntry.setState(RoutingTableEntryState.VALID);
			destinationEntry.setValidSequenceNumber(true);
			destinationEntry.setNextHopAddress(message.getInterfaceAddress());
			destinationEntry.setSequenceNumber(message.getDestinationSeq());
			destinationEntry.setHopCount(message.getHopCount());
		}
		// nalezy przekazac wiadomosc dalej
		if (!message.getOriginAddress().equals(physicalService.getLocalAddress())) {
			RoutingTableEntry originatorEntry = findRoutingTableEntryForAddress(message.getOriginAddress());
			physicalService.sendPacket(message, originatorEntry.getNextHopAddress());

			// aktualizacja adresow
			destinationEntry.getPrecursors().add(originatorEntry.getNextHopAddress());
			originatorEntry.getPrecursors().add(destinationEntry.getNextHopAddress());
		} // jezeli dany wezel jest wezlem ktory wyslal wiadomosc RREQ, nalezy
			// wyslac zalegle pakiety z danymi
		else {
			if (dataPacketsQueues.containsKey(message.getDestinationAddress())) {
				LinkedList<DataPacket> packetsQueue = dataPacketsQueues.get(message.getDestinationAddress());
				while (!packetsQueue.isEmpty()) {
					DataPacket packetToSend = packetsQueue.pollFirst();
					physicalService.sendPacket(packetToSend, destinationEntry.getNextHopAddress());
				}
			}
		}
	}

	private RREPMessage generateRrepMessageAsIntermediateNode(RREQMessage message) {
		RoutingTableEntry destinationEntry = findRoutingTableEntryForAddress(message.getDestinationAddress());
		RREPMessage rrepMessage = new RREPMessage();

		rrepMessage.setDestinationSeq(destinationEntry.getSequenceNumber());

		destinationEntry.getPrecursors().add(message.getInterfaceAddress());
		RoutingTableEntry originationEntry = findRoutingTableEntryForAddress(message.getOriginAddress());
		originationEntry.getPrecursors().add(destinationEntry.getNextHopAddress());

		rrepMessage.setOriginAddress(message.getOriginAddress());
		rrepMessage.setHopCount(destinationEntry.getHopCount());
		rrepMessage.setDestinationAddress(message.getDestinationAddress());
		return rrepMessage;
	}

	private RREPMessage generateRrepMessageAsDestination(RREQMessage message) {
		// inkrementuj wlasny sequence number jezeli seqNb w wiadomosci wiekszy
		// od twojego
		if (message.getDestinationSeq() > nodeSequenceNumber) {
			nodeSequenceNumber++;
		}
		RREPMessage rrepMessage = new RREPMessage();
		rrepMessage.setDestinationSeq(nodeSequenceNumber);
		rrepMessage.setOriginAddress(message.getOriginAddress());
		rrepMessage.setHopCount(0);
		rrepMessage.setDestinationAddress(message.getDestinationAddress());
		return rrepMessage;
	}

	private RREPMessage generateGratuitousRREP(RREQMessage message) {
		RoutingTableEntry sourceEntry = findRoutingTableEntryForAddress(message.getOriginAddress());
		RREPMessage rrepMessage = new RREPMessage();
		rrepMessage.setHopCount(sourceEntry.getHopCount());
		rrepMessage.setDestinationAddress(message.getOriginAddress());
		rrepMessage.setDestinationSeq(message.getOriginSeq());
		rrepMessage.setOriginAddress(message.getDestinationAddress());
		return rrepMessage;
	}

	private synchronized RREQMessage generateRREQMessage(RoutingTableEntry entry) {
		RREQMessage rreqMessage = new RREQMessage();
		// jezeli w tablicy rutingu ustawiony jest seqNumber to ustaw go w
		// wiadomosci RREQ
		if (entry.getSequenceNumber() != null) {
			rreqMessage.setDestinationSeq(entry.getSequenceNumber());
		} // w innym przypadku nie ustawiaj go i dodaj flage U
		else {
			rreqMessage.setFlagU(true);
			rreqMessage.setDestinationSeq(0);
		}
		rreqMessage.setDestinationAddress(entry.getDestinationNode().getAddress());
		rreqMessage.setOriginAddress(physicalService.getLocalAddress());
		rreqMessage.setOriginSeq(nodeSequenceNumber);
		rreqMessage.setId(++rreqMessageId);
		rreqMessage.setHopCount(0);
		// wartosc na sztywno zapisana w constantsach
		rreqMessage.setFlagG(AodvContants.DEFAULT_G_FLAG_IN_RREQ_VALUE);

		// wstaw do tabeli, tak aby nie odpowiadac na wlasne wiadomosci RREQ
		recentlyRecievedRreqMessagesTimestamps.put(rreqMessageId + "_" + physicalService.getLocalAddress(), new Date().getTime());
		return rreqMessage;
	}

	private class CheckRoutingTableThread extends Thread {
		private boolean active;
		private final int checkInterval;

		public CheckRoutingTableThread(int checkInterval) {
			super();
			this.checkInterval = checkInterval;
			active = true;
		}

		@Override
		public void run() {
			while (active) {
				updateRoutingTableEntries();
				try {
					Thread.sleep(checkInterval);
				} catch (InterruptedException e) {
					Log.d(TAG, "przerwano watek!");
				}
			}
		}

		public void cancel() {
			active = false;
		}
	}

	private synchronized void updateRoutingTableEntries() {
		for (RoutingTableEntry entry : routingTable) {
			// sprawdzane sa wylacznie wpisy aktywne, i dekrementowana jest ich
			// wartosc licznika
			if (RoutingTableEntryState.VALID == entry.getState()) {
				if (new Date().getTime() > entry.getValidTimestamp()) {
					makeRoutingTableEntryInvalid(entry);
				}
			}
		}
		sendNetworkBroadcast(new NetworkLayerEvent(NetworkLayerEventType.NETWORK_STATE_CHANGED));
	}

	private class RreqMessageSenderThread extends Thread {

		private final RoutingTableEntry entry;
		private boolean routeEstablished = false;

		public RreqMessageSenderThread(RoutingTableEntry entry) {
			this.entry = entry;
		}

		@Override
		public void run() {
			nodeSequenceNumber++;
			for (int i = 0; i < AodvContants.RREQ_RETRIES; i++) {
				// jesli sciezka dalej jest niepoprawna i nalezy wyslac kolejne
				// wiadomosci
				if (RoutingTableEntryState.VALID != entry.getState()) {
					RREQMessage rreqMessage = generateRREQMessage(entry);
					physicalService.sendPacketBroadcast(rreqMessage);
					try {
						Thread.sleep(AodvContants.NET_TRAVERSAL_TIME);
						// wait(AodvContants.NET_TRAVERSAL_TIME);
					} catch (InterruptedException e) {
						Log.d(TAG, "przerwano watek!");
					}
				}// w innym przypadku mozna przerwac i zakonczyc watek
				else {
					routeEstablished = true;
					break;
				}
			}
			// jesli nie udalo sie znalezc sciezki trzeba poinformowac warstwe
			// aplikacyjna
			if (!routeEstablished) {
				// ustaw wezel jako nieaktywny
				entry.setState(RoutingTableEntryState.INVALID);
				// wyczysc kolejke pakietow jesli cos sie w niej znajduje (a
				// powinno)
				if (dataPacketsQueues.get(entry.getDestinationNode().getAddress()) != null) {
					dataPacketsQueues.get(entry.getDestinationNode().getAddress()).clear();
				}
				sendNetworkBroadcast(new NetworkLayerEvent(NetworkLayerEventType.DESTINATION_UNREACHABLE, entry.getDestinationNode().getAddress()));
			}
		}
	}

	private boolean makeRoutingTableEntryValidLonger(String address) {
		boolean validityExtended = false;
		if (address != null) {
			RoutingTableEntry entry = findRoutingTableEntryForAddress(address);
			// wpis musi byc aktualny. Jezeli w miedzyczasie jakis wezel go
			// uniewaznil, to jego aktualnosc nie moze zostac przedluzona
			if (RoutingTableEntryState.VALID == entry.getState()) {
				makeRoutingTableEntryValid(entry);
				validityExtended = true;
			}
		}
		return validityExtended;
	}

	private void makeRoutingTableEntryValid(String address) {
		if (address != null && !address.equals(physicalService.getLocalAddress())) {
			RoutingTableEntry entry = findRoutingTableEntryForAddress(address);
			makeRoutingTableEntryValid(entry);
		}
	}

	private void makeRoutingTableEntryValid(RoutingTableEntry entry) {
		entry.setState(RoutingTableEntryState.VALID);
		if (nodeIsNeighbour(entry)) {
			entry.setValidTimestamp(new Date().getTime() + AodvContants.NIEGHBOUR_ACTIVE_ROUTE_TIMEOUT);
		} else {
			entry.setValidTimestamp(new Date().getTime() + AodvContants.ACTIVE_ROUTE_TIMEOUT);
		}
	}

	private void makeRoutingTableEntryInvalid(String address) {
		RoutingTableEntry entry = findRoutingTableEntryForAddress(address);
		makeRoutingTableEntryInvalid(entry);
	}

	private void makeRoutingTableEntryInvalid(RoutingTableEntry entry) {
		entry.setState(RoutingTableEntryState.INVALID);
		entry.setNextHopAddress(null);
		entry.setHopCount(null);
		if (nodeIsNeighbour(entry)) {
			entry.setValidTimestamp(new Date().getTime() + AodvContants.NIEGHBOUR_ACTIVE_ROUTE_TIMEOUT);
		} else {
			entry.setValidTimestamp(new Date().getTime() + AodvContants.ACTIVE_ROUTE_TIMEOUT);
		}
		// zwieksz numer sekwencyjny o 1
		entry.setSequenceNumber(entry.getSequenceNumber() + 1);
	}

	private boolean nodeIsNeighbour(RoutingTableEntry entry) {
		if (entry.getDestinationNode().getAddress().equals(entry.getNextHopAddress())) {
			return true;
		} else {
			return false;
		}
	}

	private boolean nodeIsNeighbour(String address) {
		RoutingTableEntry entry = findRoutingTableEntryForAddress(address);
		return nodeIsNeighbour(entry);
	}

	private RoutingTableEntryState checkNodeValidity(String address) {
		RoutingTableEntry entry = findRoutingTableEntryForAddress(address);
		return checkNodeValidity(entry);
	}

	private RoutingTableEntryState checkNodeValidity(RoutingTableEntry entry) {
		// jezli wezel jest sasiadem, wtedy zawsze jest aktualny
		if (entry.getDestinationNode().getAddress().equals(entry.getNextHopAddress())) {
			return RoutingTableEntryState.VALID;
		} else {
			// jezeli dane o wezle sa przeterminowane
			if (entry.getValidTimestamp() == null || new Date().getTime() > entry.getValidTimestamp()) {
				return RoutingTableEntryState.INVALID;
			} // jezeli caly czas sa aktualne
			else {
				return RoutingTableEntryState.VALID;
			}
		}
	}

	private void sendNetworkBroadcast(NetworkLayerEvent event) {
		Intent intent = new Intent(NETWORK_LAYER_MESSAGE);
		intent.putExtra(NETWORK_LAYER_MESSAGE_TYPE, event);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	private void registerBroadcastRecievers() {
		LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
		broadcastManager.registerReceiver(broadcastReceiver, new IntentFilter(PhysicalLayerService.PHYSICAL_LAYER_MESSAGE));

	}

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (PhysicalLayerService.PHYSICAL_LAYER_MESSAGE.equals(action)) {
				PhysicalLayerEvent event = (PhysicalLayerEvent) intent.getSerializableExtra(PhysicalLayerService.PHYSICAL_LAYER_MESSAGE_TYPE);
				handlePhysicalLayerEvent(event);
			}
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
		checkRoutingTableThread.cancel();
		nodeDao.close();
		unbindService(mConnection);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
		super.onDestroy();
	}

	@Override
	public void reInitialize() {
		physicalService.initialize();
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			physicalService = (PhysicalLayerService) ((PhysicalLayerService.MyBinder) binder).getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			physicalService = null;
		}
	};

	@Override
	public void searchForDevices() {
		physicalService.searchForNeighbours();
	}

	@Override
	public void stopSearchingForDevices() {
		physicalService.cancelSearchingForNeighbours();
	}

	@Override
	public void connectToNeighbours() {
		physicalService.connectToNeighbours();
	}

	@Override
	public List<String> getNodes() {
		List<String> returnList = new ArrayList<String>();
		for (RoutingTableEntry entry : routingTable) {
			returnList.add(entry.getDestinationNode().getAddress());
		}
		return returnList;
	}

	private RoutingTableEntry findRoutingTableEntryForAddress(String address) {
		RoutingTableEntry result = null;
		if (address != null) {
			for (RoutingTableEntry entry : routingTable) {
				if (address.equals(entry.getDestinationNode().getAddress())) {
					result = entry;
					break;
				}
			}
		}
		// jesli w tablicy rutingu jest taki wpis to go zwroc
		if (result != null) {
			return result;
		}
		// jezeli nie ma, to stworz nowy node i dodaj wpis
		else {
			Node newNode = new Node(address, "");
			long id = nodeDao.create(newNode);
			newNode.setId(id);
			result = new RoutingTableEntry(newNode);
			routingTable.add(result);
			return result;
		}
	}

	@Override
	public void sendBroadcastData(Serializable data) {
		for (RoutingTableEntry entry : routingTable) {
			if (entry != null && RoutingTableEntryState.VALID == entry.getState()) {
				physicalService.sendPacket(new DataPacket(data), entry.getNextHopAddress());
			}
		}
	}

	@Override
	public Set<RoutingTableEntry> getRoutingTable() {
		return routingTable;
	}

	@Override
	public void cancelAll() {
		physicalService.cancelAll();
	}

}

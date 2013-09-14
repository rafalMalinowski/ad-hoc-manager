package pl.rmalinowski.adhocmanager.api.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.rmalinowski.adhocmanager.api.NetworkLayerService;
import pl.rmalinowski.adhocmanager.api.PhysicalLayerService;
import pl.rmalinowski.adhocmanager.events.NetworkLayerEvent;
import pl.rmalinowski.adhocmanager.events.NetworkLayerEventType;
import pl.rmalinowski.adhocmanager.events.PhysicalLayerEvent;
import pl.rmalinowski.adhocmanager.exceptions.BadAddressException;
import pl.rmalinowski.adhocmanager.model.Node;
import pl.rmalinowski.adhocmanager.model.RoutingTableEntry;
import pl.rmalinowski.adhocmanager.model.RoutingTableEntryState;
import pl.rmalinowski.adhocmanager.model.packets.DataPacket;
import pl.rmalinowski.adhocmanager.model.packets.HelloMessage;
import pl.rmalinowski.adhocmanager.model.packets.Packet;
import pl.rmalinowski.adhocmanager.model.packets.RERRMessage;
import pl.rmalinowski.adhocmanager.model.packets.RREPAckMessage;
import pl.rmalinowski.adhocmanager.model.packets.RREPMessage;
import pl.rmalinowski.adhocmanager.model.packets.RREQMessage;
import pl.rmalinowski.adhocmanager.model.packets.RoutingPacket;
import pl.rmalinowski.adhocmanager.persistence.NodeDao;
import pl.rmalinowski.adhocmanager.utils.AodvContants;
import android.app.Service;
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
	private List<RoutingTableEntry> routingTable;
	private volatile Map<String, LinkedList<DataPacket>> dataPacketsQueues;
	private NodeDao nodeDao;
	private volatile Integer nodeSequenceNumber;
	private volatile Integer rreqMessageId;
	private Map<Integer, Long> recentlyRecievedRreqMessagesTimestamps;

	@Override
	public void onCreate() {
		super.onCreate();
		bindService(new Intent(this, BluetoothService.class), mConnection, Context.BIND_AUTO_CREATE);
		registerBroadcastRecievers();
		nodeDao = new NodeDao(this);
		nodeDao.open();
	}

	private void initializeNetworkLayer() {
		nodeSequenceNumber = 0;
		rreqMessageId = 0;
		dataPacketsQueues = new HashMap<String, LinkedList<DataPacket>>();
		recentlyRecievedRreqMessagesTimestamps = new HashMap<Integer, Long>();
		initializeRoutingTable();
	}

	private void initializeRoutingTable() {
		routingTable = new ArrayList<RoutingTableEntry>();
		Set<Node> nodes = nodeDao.getAllNodes();
		for (Node node : nodes) {
			RoutingTableEntry entry = new RoutingTableEntry(node);
			routingTable.add(entry);
		}
	}

	@Override
	public void sendData(Serializable data, String address) {
		RoutingTableEntry entry = findRoutingTableEntryForAddress(address);
		RoutingTableEntryState entryState = checkNodeValidity(entry);
		switch (entryState) {
		// jesli wpis o wezle docelowym jest aktualny
		case VALID:
			// wyslij wiadomosc
			physicalService.sendPacket(new DataPacket(data), address);
			// zaktualizuj dane o wezle docelowym
			// TODO czy aby na pewno?? sprawdzic
			nodeIsValidUpdate(address);
			break;
		// jesli jest nieaktualny
		case INVALID:
			// zdeaktualizuj wezel
			entry.setState(entryState);
			// rozpocznij watek wysylajacy wiadomosci RREQ
			RreqMessageSenderThread rreqMessageSender = new RreqMessageSenderThread(entry);
			rreqMessageSender.start();
			// dodaj pakiety do kolejnki oczekujacej na wyslanie
			addDataPacketToWaitingQueue(new DataPacket(data), address);
			// ustaw status wezla na walidowany
			entry.setState(RoutingTableEntryState.VALIDATING);
			break;
		// jesli juz zostaly wyslane wiadomosci typu RREQ
		case VALIDATING:
			// dodaj pakiety do kolejnki oczekujacej na wyslanie
			addDataPacketToWaitingQueue(new DataPacket(data), address);
			break;
		default:
			break;
		}
	}

	private void addDataPacketToWaitingQueue(DataPacket packet, String address) {
		// jezeli kolejka juz istnieje dodaj element
		if (dataPacketsQueues.containsKey(address)) {
			dataPacketsQueues.get(address).addLast(packet);
		} // jezeli kolejka nie istnieje, stworz ja i dodaj element
		else {
			LinkedList<DataPacket> list = new LinkedList<DataPacket>();
			list.addLast(packet);
			dataPacketsQueues.put(address, list);
		}
	}

	private void handlePhysicalLayerEvent(PhysicalLayerEvent event) {
		switch (event.getEventType()) {
		case ADAPTER_NOT_ENABLED:
			sendNetworkBroadcast(new NetworkLayerEvent(NetworkLayerEventType.ADAPTED_DISABLED, event.getData()));
			break;
		case CONNECTION_TO_NEIGHBOUR_ESTABLISHED:
			String address = (String) event.getData();
			RoutingTableEntry entry = findRoutingTableEntryForAddress(address);
			entry.setState(RoutingTableEntryState.VALID);
			entry.setHopCount(1);
			entry.setNextHopAddress(address);
			break;
		case PHYSICAL_LAYER_INITIALIZED:
			initializeNetworkLayer();
			physicalService.connectToNeighbours();
			break;
		case CONNECTING_TO_NEIGHBOURS_FINISHED:
			sendNetworkBroadcast(new NetworkLayerEvent(NetworkLayerEventType.SHOW_TOAST, "uruchomiono wartswe fizyczna"));
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
	}

	private void handleRecievedDataPacket(DataPacket dataPacket) {
		// jezeli wezel jest adresatem wiadomosci
		if (physicalService.getLocalAddress().equals(dataPacket.getDestinationAddress())) {
			// wyslij informacje do wyzszych warstw
			sendNetworkBroadcast(new NetworkLayerEvent(NetworkLayerEventType.DATA_RECIEVED, dataPacket.getData()));
			// zaktualizuj dane o wezle ktory wyslal wiadomosc
			nodeIsValidUpdate(dataPacket.getSourceAddress());
			// jezeli sasiad od ktorego otrzymano wiadomosc nie jest nadawca
			// zaktualizuj dane takze o nim
			if (!dataPacket.getSourceAddress().equals(dataPacket.getInterfaceAddress())) {
				nodeIsValidUpdate(dataPacket.getInterfaceAddress());
			}
		} // jezeli wezel nie jest adresatem wiadomosci
		else {
			RoutingTableEntry destinationEntry = findRoutingTableEntryForAddress(dataPacket.getDestinationAddress());
			// jezeli wpis w tabeli routingu do wezla docelowego jest aktualny
			if (RoutingTableEntryState.VALID == destinationEntry.getState()) {
				// przekaz wiadomosc dalej do urzadzenia pobranego z tabeli
				// routingu
				physicalService.sendPacket(dataPacket, destinationEntry.getNextHopAddress());
				// nastepnie zaktualizuj informacje o wezle ktory wyslal
				// wiadomosc
				nodeIsValidUpdate(dataPacket.getSourceAddress());
				// jezeli sasiad od ktorego otrzymano wiadomosc nie jest nadawca
				// zaktualizuj dane takze o nim
				if (!dataPacket.getSourceAddress().equals(dataPacket.getInterfaceAddress())) {
					nodeIsValidUpdate(dataPacket.getInterfaceAddress());
				}
				// zaktualizuj takze informacje o adresacie
				nodeIsValidUpdate(dataPacket.getDestinationAddress());
				// jezeli wezel do ktorego przekazywana jest wiadomosc nie jest
				// adresatem, zaktualizuj takze dane o nim
				if (!dataPacket.getDestinationAddress().equals(destinationEntry.getNextHopAddress())) {
					nodeIsValidUpdate(destinationEntry.getNextHopAddress());
				}
			} // inaczej trzeba wyslac wiadomosc RERR
			else {
				// TODO obsluzyc wiadomosci RERR
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
		} else if (message instanceof RREPAckMessage) {
			handleRREPAckMessage((RREPAckMessage) message);
		} else if (message instanceof HelloMessage) {
			handleHelloMessage((HelloMessage) message);
		}
	}

	private void handleRERRMessage(RERRMessage message) {

	}

	private void handleRREQMessage(RREQMessage message) {
		// TODO zaktualizuj numer sekwencyjny wezla od ktorego dostalej
		// wiadomosc - nie rozumiem jeszcze o co chodzi

		// jesli parametr TTL wiadomosci wymosi 1 lub mniej to nic nie rob
		if (message.getTtl() <= 1) {
			return;
		}

		// sprawdz czy dostales juz RREQ o podanym id
		if (recentlyRecievedRreqMessagesTimestamps.containsKey(message.getId())) {
			long lastRreqMessageTimestamp = recentlyRecievedRreqMessagesTimestamps.get(message.getId());
			// jesli dana wiadomosc byla uzyskana niedawno to przerwij dalsze
			// wywolywanie metody
			if (new Date().getTime() < (lastRreqMessageTimestamp + AodvContants.PATH_DISCOVERY_TIME)) {
				return;
			}
		} // w innym przypadku dodaj do mapy odpowiedni wpis
		else {
			recentlyRecievedRreqMessagesTimestamps.put(message.getId(), new Date().getTime());
		}
		// inkrementuj liczbe skokow
		message.setHopCount(message.getHopCount() + 1);

		RoutingTableEntry sourceEntry = findRoutingTableEntryForAddress(message.getOriginAddress());
		// zaktualizuj sciezke do nadawcy jesli nie jest to bezposredni sasiad
		if (!sourceEntry.getDestinationNode().getAddress().equals(sourceEntry.getNextHopAddress())) {
			// jesli numer sekwencjny przeslany w wiadomosci jest wiekszy niz
			// ten w tablicy, nalezy zakutalizowac wpis w tablicy
			if (message.getOriginSeq() > sourceEntry.getSequenceNumber()) {
				sourceEntry.setSequenceNumber(message.getOriginSeq());
			}
			// zaktualizuj pola w tabeli rutingowej
			sourceEntry.setNextHopAddress(message.getInterfaceAddress());
			sourceEntry.setHopCount(message.getHopCount());
			nodeIsValidUpdate(sourceEntry);
		}

		RoutingTableEntry destinationEntry = findRoutingTableEntryForAddress(message.getDestinationAddress());
		// aktualizujemy status wpisu w tablicy rutingowej dotyczacy wpisu
		// docelowego
		RoutingTableEntryState destinationEntryState = checkNodeValidity(destinationEntry);
		switch (checkNodeValidity(destinationEntry)) {
		case VALID:

			break;
		// wezel nie zna sciezki, wiec przesyla wiadomosci RREQ dalej
		case INVALID:
		case VALIDATING:
			// jesli seqN w tablicy rutingowej jest wiekszy niz w wiadomosci,
			// nastepuje update
			if (destinationEntry.getSequenceNumber() > message.getDestinationSeq()) {
				message.setDestinationSeq(destinationEntry.getSequenceNumber());
			}
			 physicalService.sendPacketBroadcast(message);
			break;
		default:
			break;
		}

	}

	private void handleRREPMessage(RREPMessage message) {

	}

	private void handleRREPAckMessage(RREPAckMessage message) {

	}

	private void handleHelloMessage(HelloMessage message) {

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
		}
		rreqMessage.setDestinationAddress(entry.getDestinationNode().getAddress());
		rreqMessage.setOriginAddress(physicalService.getLocalAddress());
		rreqMessage.setOriginSeq(nodeSequenceNumber);
		rreqMessage.setId(++rreqMessageId);
		rreqMessage.setHopCount(0);
		// wartosc na sztywno zapisana w constantsach
		rreqMessage.setFlagG(AodvContants.DEFAULT_G_FLAG_IN_RREQ_VALUE);
		return rreqMessage;
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
						wait(AodvContants.NET_TRAVERSAL_TIME);
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
				entry.setState(RoutingTableEntryState.INVALID);
				sendNetworkBroadcast(new NetworkLayerEvent(NetworkLayerEventType.DESTINATION_UNREACHABLE, entry.getDestinationNode().getAddress()));
			}
		}
	}

	private void nodeIsValidUpdate(String address) {
		RoutingTableEntry entry = findRoutingTableEntryForAddress(address);
		nodeIsValidUpdate(entry);
	}

	private void nodeIsValidUpdate(RoutingTableEntry entry) {
		entry.setState(RoutingTableEntryState.VALID);
		entry.setValidTimestamp(new Date().getTime() + AodvContants.ACTIVE_ROUTE_TIMEOUT);
	}

	private RoutingTableEntryState checkNodeValidity(String address) {
		RoutingTableEntry entry = findRoutingTableEntryForAddress(address);
		return checkNodeValidity(entry);
	}

	/**
	 * metoda sprawdza czy wpis w tablicy rutingu o danym wezle jest ciagle
	 * aktualny
	 * 
	 * @param entry
	 * @return
	 */
	private RoutingTableEntryState checkNodeValidity(RoutingTableEntry entry) {
		// jezli wezel jest sasiadem, wtedy zawsze jest aktualny
		if (entry.getDestinationNode().getAddress().equals(entry.getNextHopAddress())) {
			return RoutingTableEntryState.VALID;
		} else {
			// jezeli dane o wezle sa przeterminowane
			if (new Date().getTime() > entry.getValidTimestamp()) {
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
	public int onStartCommand(Intent intent, int flags, int startId) {
		return Service.START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
		nodeDao.close();
		unbindService(mConnection);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
		stopService(new Intent(this, BluetoothService.class));
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
		for (RoutingTableEntry entry : routingTable) {
			if (address.equals(entry.getDestinationNode().getAddress())) {
				return entry;
			}
		}
		throw new BadAddressException();
	}

	@Override
	public void sendBroadcastData(Serializable data) {
		for (RoutingTableEntry entry : routingTable) {
			if (entry != null && RoutingTableEntryState.VALID == entry.getState()) {
				physicalService.sendPacket(new DataPacket(data), entry.getDestinationNode().getAddress());
			}
		}
	}

}

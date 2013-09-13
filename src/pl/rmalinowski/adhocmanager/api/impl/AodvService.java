package pl.rmalinowski.adhocmanager.api.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import pl.rmalinowski.adhocmanager.api.NetworkLayerService;
import pl.rmalinowski.adhocmanager.api.PhysicalLayerService;
import pl.rmalinowski.adhocmanager.events.NetworkLayerEvent;
import pl.rmalinowski.adhocmanager.events.NetworkLayerEventType;
import pl.rmalinowski.adhocmanager.events.PhysicalLayerEvent;
import pl.rmalinowski.adhocmanager.exceptions.BadAddressException;
import pl.rmalinowski.adhocmanager.model.Node;
import pl.rmalinowski.adhocmanager.model.RoutingTableEntry;
import pl.rmalinowski.adhocmanager.model.packets.DataPacket;
import pl.rmalinowski.adhocmanager.model.packets.HelloMessage;
import pl.rmalinowski.adhocmanager.model.packets.Packet;
import pl.rmalinowski.adhocmanager.model.packets.RERRMessage;
import pl.rmalinowski.adhocmanager.model.packets.RREPAckMessage;
import pl.rmalinowski.adhocmanager.model.packets.RREPMessage;
import pl.rmalinowski.adhocmanager.model.packets.RREQMessage;
import pl.rmalinowski.adhocmanager.model.packets.RoutingPacket;
import pl.rmalinowski.adhocmanager.persistence.NodeDao;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

public class AodvService extends NetworkLayerService {
	private static final String TAG = "AodvService";

	private final IBinder mBinder = new MyBinder();

	private PhysicalLayerService physicalService;
	private List<RoutingTableEntry> routingTable;
	private NodeDao nodeDao;

	@Override
	public void onCreate() {
		super.onCreate();
		bindService(new Intent(this, BluetoothService.class), mConnection, Context.BIND_AUTO_CREATE);
		registerBroadcastRecievers();
		nodeDao = new NodeDao(this);
		nodeDao.open();
	}

	@Override
	public void sendData(Serializable data, String address) {
		RoutingTableEntry entry = findRoutingTableEntryForAddress(address);
		if (entry != null && entry.isValid()) {
			physicalService.sendPacket(new DataPacket(data), address);
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
			entry.setValid(true);
			entry.setHopCount(1);
			entry.setNextHopAddress(address);
			break;
		case PHYSICAL_LAYER_INITIALIZED:
			initializeRoutingTable();
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
			handleRecievedRoutingPacket((DataPacket) packet);
		} else if (packet instanceof RoutingPacket) {
			handleRecievedDataPacket((RoutingPacket) packet);
		}
	}

	private void handleRecievedRoutingPacket(DataPacket dataPacket) {
		String message = (String) dataPacket.getData();
		sendNetworkBroadcast(new NetworkLayerEvent(NetworkLayerEventType.SHOW_TOAST, "otrzymalem pakiet: " + message));
	}

	private void handleRecievedDataPacket(RoutingPacket message) {
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

	}

	private void handleRREPMessage(RREPMessage message) {

	}

	private void handleRREPAckMessage(RREPAckMessage message) {

	}

	private void handleHelloMessage(HelloMessage message) {

	}

	private void sendNetworkBroadcast(NetworkLayerEvent event) {
		Intent intent = new Intent(NETWORK_LAYER_MESSAGE);
		intent.putExtra(NETWORK_LAYER_MESSAGE_TYPE, event);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	private void initializeRoutingTable() {
		routingTable = new ArrayList<RoutingTableEntry>();
		Set<Node> nodes = nodeDao.getAllNodes();
		for (Node node : nodes) {
			RoutingTableEntry entry = new RoutingTableEntry(node);
			routingTable.add(entry);
		}
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
			if (entry != null && entry.isValid()) {
				physicalService.sendPacket(new DataPacket(data), entry.getDestinationNode().getAddress());
			}
		}
	}

}

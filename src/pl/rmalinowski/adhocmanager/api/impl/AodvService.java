package pl.rmalinowski.adhocmanager.api.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import pl.rmalinowski.adhocmanager.api.NetworkLayerService;
import pl.rmalinowski.adhocmanager.api.PhysicalLayerService;
import pl.rmalinowski.adhocmanager.exceptions.BadAddressException;
import pl.rmalinowski.adhocmanager.model.NetworkLayerEvent;
import pl.rmalinowski.adhocmanager.model.NetworkLayerEventType;
import pl.rmalinowski.adhocmanager.model.Node;
import pl.rmalinowski.adhocmanager.model.PhysicalLayerEvent;
import pl.rmalinowski.adhocmanager.model.RoutingTableEntry;
import pl.rmalinowski.adhocmanager.model.packets.DataPacket;
import pl.rmalinowski.adhocmanager.persistence.NodeDao;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
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
		if (entry != null && entry.isValid()){
			physicalService.sendPacket(new DataPacket(data), address);
		}
	}
	
	

	private void handlePhysicalLayerEvent(PhysicalLayerEvent event) {
		switch (event.getEventType()) {
		case BLUETOOTH_NOT_ENABLED:
			sendNetworkBroadcast(new NetworkLayerEvent(NetworkLayerEventType.ADAPTED_DISABLED, BluetoothAdapter.ACTION_REQUEST_ENABLE));
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
		case DATA_PACKET_RECEIVED:
			DataPacket packet = (DataPacket) event.getData();
			String message = (String)packet.getData();
			sendNetworkBroadcast(new NetworkLayerEvent(NetworkLayerEventType.SHOW_TOAST, "otrzymalem pakiet: " + message));
		default:
			break;
		}
	}

	private void sendNetworkBroadcast(NetworkLayerEvent event) {
		Intent intent = new Intent(NETWORK_LAYER_MESSAGE);
		intent.putExtra(NETWORK_LAYER_MESSAGE_TYPE, event);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	// TODO do poprawy, zeby nie korzystal z bluetoothAdaptera tylko wyciagal z bazy danych
	private void initializeRoutingTable() {
		routingTable = new ArrayList<RoutingTableEntry>();
		BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
		Set<BluetoothDevice> bondedDevices = ba.getBondedDevices();
		for (BluetoothDevice bd : bondedDevices) {
			RoutingTableEntry entry = new RoutingTableEntry();
			Node node = new Node();
			node.setAddress(bd.getAddress());
			entry.setSequenceNumber(0);
			entry.setDestinationNode(node);
			entry.setValid(false);
			routingTable.add(entry);
		}
		
		Set<BluetoothDevice> bonded = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
		Node node = null;
		for (BluetoothDevice device : bonded){
			node = nodeDao.getByMacAddress(device.getAddress());
		}
		Cursor cursor = nodeDao.getAll();
		nodeDao.create(new Node("aaaa", "bbbb"));
		List<Node> nodes = nodeDao.getAllNodes();
		if (nodes.size()>0){
			
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
	public String getText() {
		return "OK!";
	}

	@Override
	public void onDestroy() {
		nodeDao.close();
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
			if (address.equals(entry.getDestinationNode().getAddress())){
				return entry;
			}
		}
		throw new BadAddressException();
	}

	@Override
	public void sendBroadcastData(Serializable data) {
		for (RoutingTableEntry entry : routingTable) {
			if (entry != null && entry.isValid()){
				physicalService.sendPacket(new DataPacket(data), entry.getDestinationNode().getAddress());
			}
		}
	}

}

package pl.rmalinowski.adhocmanager.api.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import pl.rmalinowski.adhocmanager.api.PhysicalLayerService;
import pl.rmalinowski.adhocmanager.events.PhysicalLayerEvent;
import pl.rmalinowski.adhocmanager.events.PhysicalLayerEventType;
import pl.rmalinowski.adhocmanager.model.Node;
import pl.rmalinowski.adhocmanager.model.PhysicalLayerState;
import pl.rmalinowski.adhocmanager.model.packets.Packet;
import pl.rmalinowski.adhocmanager.persistence.NodeDao;
import pl.rmalinowski.adhocmanager.utils.AodvContants;
import pl.rmalinowski.adhocmanager.utils.SerializationUtils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class WiFiDirectService extends PhysicalLayerService implements PeerListListener, ConnectionInfoListener {

	private final IBinder mBinder = new MyBinder();
	private static final String TAG = "WiFiDirectService";
	private volatile PhysicalLayerState state;
	private NodeDao nodeDao;
	private WifiP2pManager mManager;
	private Channel mChannel;
	private WifiP2pDevice localDevice;
	private Map<String, ConnectionThread> connectedDevices;

	@Override
	public void onCreate() {
		registerBroadcastRecievers();
		initialize();
		nodeDao = new NodeDao(this);
		nodeDao.open();
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		return START_NOT_STICKY;
	}

	private void registerBroadcastRecievers() {
		IntentFilter mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

		this.registerReceiver(broadcastReceiver, mIntentFilter);
		
		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(PhysicalLayerService.PHYSICAL_LAYER_MESSAGE));
	}

	@Override
	public void initialize() {
		state = PhysicalLayerState.INITIALIZING;
		mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		mChannel = (Channel) mManager.initialize(this, getMainLooper(), null);
		connectedDevices = new HashMap<String, ConnectionThread>();
		sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.PHYSICAL_LAYER_INITIALIZED));
	}

	@Override
	public void sendPacket(Packet packet, String destination) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendPacketBroadcast(Packet packet) {
		// TODO Auto-generated method stub

	}

	@Override
	public void searchForNeighbours() {
		mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
				Log.d(TAG, "udalo sie rozpoczecie poszukiwan");
			}

			@Override
			public void onFailure(int reasonCode) {
				Log.d(TAG, "nie udalo sie rozpoczecie poszukiwan");
			}
		});
	}
	
	@Override
	public void cancelSearchingForNeighbours() {
		mManager.stopPeerDiscovery(mChannel, new ActionListener() {
			
			@Override
			public void onSuccess() {
				Log.d(TAG, "udalo sie anulowanie poszukiwan");
				
			}
			
			@Override
			public void onFailure(int reason) {
				Log.d(TAG, "nie udalo sie anulowanie poszukiwan");
				
			}
		});
	}

	@Override
	public void connectToNeighbours() {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<Node> getConnectedDevices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLocalAddress() {
		return localDevice.deviceAddress;
	}

	@Override
	public void sendPacketBroadcastExceptOneAddress(Packet packet, String address) {
		// TODO Auto-generated method stub

	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
				int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
				if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
					Log.d(TAG, "jest ok");
				} else {
					Log.d(TAG, "srednio :/");
				}

				// metoda uruchamiana, gdy zostanie znalezione nowe urzadzenie
			} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
				Log.d(TAG, "Broadcast: WIFI_P2P_PEERS_CHANGED_ACTION");
				// jedyne co robimy w tej metodzie to uruchamiany asynchroniczne
				// zadanie pobrania listy dostepnych urzadzen
				mManager.requestPeers(mChannel, WiFiDirectService.this);

			} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
				NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
				if (networkInfo.isConnected()) {
					mManager.requestConnectionInfo(mChannel, WiFiDirectService.this);
				}
			} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
				localDevice = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
			} else if (PhysicalLayerService.PHYSICAL_LAYER_MESSAGE.equals(action)) {
				Log.d(TAG, "test");
			}

		}
	};

	@Override
	public void onDestroy() {
		if (nodeDao != null){
			nodeDao.close();			
		}
		unregisterReceiver(broadcastReceiver);
		super.onDestroy();
	}

	@Override
	public void connectToDevice(final String address, final int numberOfRetries) {
		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = address;
		mManager.connect(mChannel, config, new ActionListener() {

			@Override
			public void onSuccess() {
				// udalo sie wyslac zadanie

			}

			@Override
			public void onFailure(int reason) {
				// nie udalo wyslac sie zadania, tak wiec ponow probe o ile
				// maksymalna liczba prob nie zostala wykorzystana
				int decreasedNumberOfRetries = numberOfRetries - 1;
				if (decreasedNumberOfRetries > 0) {
					WiFiDirectService.this.connectToDevice(address, --decreasedNumberOfRetries);
				}
			}
		});

	}

	@Override
	public void disconnectFromDevice(String address) {
		// TODO Auto-generated method stub

	}

	private void sendPhysicalBroadcast(PhysicalLayerEvent event) {
		Intent intent = new Intent(PHYSICAL_LAYER_MESSAGE);
		intent.putExtra(PHYSICAL_LAYER_MESSAGE_TYPE, event);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	private synchronized void startCommunication(Socket socket) {
		// sendPhysicalBroadcast(new
		// PhysicalLayerEvent(PhysicalLayerEventType.CONNECTION_TO_NEIGHBOUR_ESTABLISHED,
		// socket.));
		ConnectionThread connectionThread = new ConnectionThread(socket);
		connectionThread.start();
	}

	// ////////////////// WIFI DIRECT INTERFACES METHODS //////////////////////

	@Override
	public void onPeersAvailable(WifiP2pDeviceList peers) {

		for (WifiP2pDevice device : peers.getDeviceList()) {
			Boolean deviceAlreadyKnown = false;
			for (Node node : nodeDao.getAllNodes()) {
				if (node.getAddress().equals(device.deviceAddress)) {
					deviceAlreadyKnown = true;
					break;
				}
			}

			if (!deviceAlreadyKnown) {
				Node newNode = new Node();
				newNode.setAddress(device.deviceAddress);
				newNode.setName(device.deviceName);
				long id = nodeDao.create(newNode);
				newNode.setId(id);
				sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.NEW_NODE_ADDED, newNode));
			}
		}

	}

	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info) {
		String groupOwnerAddress = info.groupOwnerAddress.getHostAddress();
		// czy udalo sie utworzyc grupe z drugim urzadzeniem
		if (info.groupFormed) {
			// jezeli wezel jest wlascicielem grupy to powinien rozpoczac
			// nasluchiwanie nowego polaczenia
			if (info.isGroupOwner) {
				GroupOwnerThread groupOwnerThread = new GroupOwnerThread(groupOwnerAddress);
				groupOwnerThread.start();
			}
			// jezeli nie jest wlascicielem grupy to powinien rozpoczac probe
			// polaczenia sie z drugim wezlem
			else {
				ClientThread clientThread = new ClientThread(groupOwnerAddress);
				clientThread.start();
			}
		}

	};

	// ///////////////// watki odpowiedzialne za komunikacje ///////////////////

	private class GroupOwnerThread extends Thread {
		private String groupOwnerAddress;
		private ServerSocket socket = null;

		public GroupOwnerThread(String groupOwnerAddress) {
			this.groupOwnerAddress = groupOwnerAddress;
		}

		@Override
		public void run() {
			try {
				socket = new ServerSocket(4545);
				Socket acceptedSocket = socket.accept();
				startCommunication(acceptedSocket);
			} catch (IOException e) {
				try {
					if (socket != null && !socket.isClosed())
						socket.close();
				} catch (IOException ioe) {

				}
			}
		}

	}

	private class ClientThread extends Thread {
		private String groupOwnerAddress;

		public ClientThread(String groupOwnerAddress) {
			this.groupOwnerAddress = groupOwnerAddress;
		}

		@Override
		public void run() {
			Socket socket = new Socket();
			try {
				socket.bind(null);
				socket.connect(new InetSocketAddress(groupOwnerAddress, 4545), 5000);
				startCommunication(socket);

			} catch (IOException e) {
				e.printStackTrace();
				try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				return;
			}
		}

	}

	private class ConnectionThread extends Thread {

		private Socket socket;
		private InputStream iStream;
		private OutputStream oStream;

		public ConnectionThread(Socket socket) {
			super();
			this.socket = socket;
		}

		@Override
		public void run() {
			try {
				iStream = socket.getInputStream();
				oStream = socket.getOutputStream();
				byte[] buffer = new byte[1024];
				int bytes;

				while (true) {
					try {
						bytes = iStream.read(buffer);
						if (bytes == -1) {
							break;
						}
						Packet packet = (Packet) SerializationUtils.deserialize(buffer);
						sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.PACKET_RECEIVED, packet));

						Log.d(TAG, "Rec:" + String.valueOf(buffer));

					} catch (IOException e) {
						Log.e(TAG, "disconnected", e);
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		public void write(Packet packet) {
			try {
				// przed wyslaniem pakietu ustaw interfejs i dekrementuj pole
				// TTL
				if (packet.getTtl() != null) {
					packet.setTtl(packet.getTtl() - 1);
				} else {
					packet.setTtl(AodvContants.TTL_VALUE);
				}
				packet.setInterfaceAddress(localDevice.deviceAddress);
				byte[] buffer = SerializationUtils.serialize(packet);
				oStream.write(buffer);
				sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.PACKET_SEND, packet));
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}

	}

}

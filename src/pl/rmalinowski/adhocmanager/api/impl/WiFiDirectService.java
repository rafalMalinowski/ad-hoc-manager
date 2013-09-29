package pl.rmalinowski.adhocmanager.api.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pl.rmalinowski.adhocmanager.api.PhysicalLayerService;
import pl.rmalinowski.adhocmanager.events.PhysicalLayerEvent;
import pl.rmalinowski.adhocmanager.events.PhysicalLayerEventType;
import pl.rmalinowski.adhocmanager.model.Node;
import pl.rmalinowski.adhocmanager.model.PhysicalLayerState;
import pl.rmalinowski.adhocmanager.model.packets.Packet;
import pl.rmalinowski.adhocmanager.model.packets.WifiHelloPacket;
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
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class WiFiDirectService extends PhysicalLayerService implements PeerListListener, ConnectionInfoListener, ChannelListener, GroupInfoListener {

	private static final int START_CONTINUE = 1;
	private static final int STOP = 2;

	private final IBinder mBinder = new MyBinder();
	private static final String TAG = "WiFiDirectService";
	private volatile PhysicalLayerState state;
	private NodeDao nodeDao;
	private WifiP2pManager mManager;
	private Channel mChannel;
	// private Channel mChannel2;
	private WifiP2pDevice localDevice;
	private Map<String, ConnectionThread> connectedDevices;
	private GroupOwnerThread groupOwnerThread = null;
	private Map<String, String> macAddresToIpAddress;

	@Override
	public void onCreate() {
		registerBroadcastRecievers();
		initialize();
		nodeDao = new NodeDao(this);
		nodeDao.open();
		super.onCreate();
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
		// mChannel2 = (Channel) mManager.initialize(this, getMainLooper(),
		// null);
		connectedDevices = new HashMap<String, ConnectionThread>();
		macAddresToIpAddress = new HashMap<String, String>();
		sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.PHYSICAL_LAYER_INITIALIZED));
	}

	@Override
	public void sendPacket(Packet packet, String destination) {
		if (connectedDevices != null && connectedDevices.containsKey(destination)) {
			connectedDevices.get(destination).write(packet);
		}
	}

	@Override
	public void sendPacketBroadcast(Packet packet) {
		if (connectedDevices != null) {
			for (ConnectionThread thread : connectedDevices.values()) {
				thread.write(packet);
			}
		}
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
				Log.d(TAG, "nie udalo sie rozpoczecie poszukiwan, powod : " + reasonCode);
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
		Set<Node> returnNodes = new HashSet<Node>();
		for (Node node : nodeDao.getAllNodes()) {
			if (isCommunicationWithDeviceActive(node.getAddress())) {
				returnNodes.add(node);
			}
		}
		return returnNodes;
	}

	@Override
	public String getLocalAddress() {
		return localDevice.deviceAddress;
	}

	@Override
	public void sendPacketBroadcastExceptOneAddress(Packet packet, String address) {
		for (ConnectionThread thread : connectedDevices.values()) {
			if (!address.equals(thread.getAddress())) {
				thread.write(packet);
			}
		}
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
				Log.d(TAG, "Broadcast: WIFI_P2P_STATE_CHANGED_ACTION");
				int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
				if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
					Log.d(TAG, "jest ok");
					sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.TOAST, "Wifi Direct enabled"));
				} else {
					Log.d(TAG, "srednio :/");
					sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.TOAST, "Wifi Direct disabled"));
				}

				// metoda uruchamiana, gdy zostanie znalezione nowe urzadzenie
			} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
				Log.d(TAG, "Broadcast: WIFI_P2P_PEERS_CHANGED_ACTION");
				// jedyne co robimy w tej metodzie to uruchamiany asynchroniczne
				// zadanie pobrania listy dostepnych urzadzen
				mManager.requestPeers(mChannel, WiFiDirectService.this);

			} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
				Log.d(TAG, "Broadcast: WIFI_P2P_CONNECTION_CHANGED_ACTION");
				NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
				if (networkInfo.isConnected()) {
					mManager.requestConnectionInfo(mChannel, WiFiDirectService.this);
				}
			} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
				Log.d(TAG, "Broadcast: WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
				localDevice = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
			} else if (PhysicalLayerService.PHYSICAL_LAYER_MESSAGE.equals(action)) {
				Log.d(TAG, "test");
			}

		}
	};

	@Override
	public void onDestroy() {
		try {
			stopAllThreads();
			if (mManager != null) {
				mManager.removeGroup(mChannel, null);
			}

			if (nodeDao != null) {
				nodeDao.close();
			}
			unregisterReceiver(broadcastReceiver);
		} catch (Exception e) {
			Log.d(TAG, "blad przy niszczeniu serwisu");
		}
		super.onDestroy();
	}

	private void stopAllThreads() {

		if (connectedDevices != null) {
			for (ConnectionThread thread : connectedDevices.values()) {
				thread.cancel();
			}
		}
		connectedDevices = null;
	}

	@Override
	public void connectToDevice(final String address, final int numberOfRetries) {
		if (macAddresToIpAddress.containsKey(address)) {
			ClientThread clientThread = new ClientThread(address);
			clientThread.start();
		} else {
			WifiP2pConfig config = new WifiP2pConfig();
			config.deviceAddress = address;
			mManager.connect(mChannel, config, new ActionListener() {

				@Override
				public void onSuccess() {
					Log.d(TAG, "udalo sie rozpoczecie polaczenia");
				}

				@Override
				public void onFailure(int reason) {
					Log.d(TAG, "nie udalo sie rozpoczecie polaczenia, powod: " + reason);
					// nie udalo wyslac sie zadania, tak wiec ponow probe o ile
					// maksymalna liczba prob nie zostala wykorzystana
					int decreasedNumberOfRetries = numberOfRetries - 1;
					if (decreasedNumberOfRetries > 0) {
						WiFiDirectService.this.connectToDevice(address, --decreasedNumberOfRetries);
					}
				}
			});
		}
	}

	@Override
	public void disconnectFromDevice(String address) {
		if (isCommunicationWithDeviceActive(address)) {
			ConnectionThread connectionThread = connectedDevices.get(address);
			connectionThread.cancel();
		}

	}

	private boolean isCommunicationWithDeviceActive(String address) {
		if (connectedDevices.containsKey(address)) {
			return true;
		} else {
			return false;
		}
	}

	private void sendPhysicalBroadcast(PhysicalLayerEvent event) {
		Intent intent = new Intent(PHYSICAL_LAYER_MESSAGE);
		intent.putExtra(PHYSICAL_LAYER_MESSAGE_TYPE, event);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	private Handler connectionsStatusesChecker = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (START_CONTINUE == msg.what) {
				for (ConnectionThread connectionThread : connectedDevices.values()) {
					if (!connectionThread.isValid()) {
						connectionThread.cancel();
					}
				}
				this.sendEmptyMessageDelayed(START_CONTINUE, 1000);
			}
		}
	};

	private Handler helloMessageHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			ConnectionThread connectionThread = (ConnectionThread) msg.obj;
			WifiHelloPacket helloPacket = new WifiHelloPacket(localDevice.deviceAddress);
			connectionThread.write(helloPacket);
		}
	};

	private synchronized void startCommunication(Socket socket) {

		final ConnectionThread connectionThread = new ConnectionThread(socket);
		connectionThread.start();

		Message message = new Message();
		message.obj = connectionThread;
		// po sekundzie wyslij wiadomosc hello
		helloMessageHandler.sendMessageDelayed(message, 100);
	}

	private synchronized void handleDisconnect(String macAddress) {
		sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.CONNECTION_TO_NEIGHBOUR_LOST, macAddress));
		if (connectedDevices.containsKey(macAddress)) {
			connectedDevices.remove(macAddress);
		}
	}

	private synchronized void newConnectionEstablished(ConnectionThread connectionThread, String macAddress, String ipAddress) {
		connectedDevices.put(macAddress, connectionThread);
		sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.CONNECTION_TO_NEIGHBOUR_ESTABLISHED, macAddress));
		sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.TOAST, macAddress));
		macAddresToIpAddress.put(macAddress, ipAddress);
		connectionsStatusesChecker.sendEmptyMessage(START_CONTINUE);
	}

	public static String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
						return inetAddress.getHostAddress();
					}
				}
			}
		} catch (SocketException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	// ////////////////// WIFI DIRECT INTERFACES METHODS //////////////////////

	@Override
	public void onPeersAvailable(WifiP2pDeviceList peers) {
		Log.d(TAG, "onPeersAvailable. Dostepne peery:");
		getLocalIpAddress();
		sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.TOAST, "New peers found. Total size:" + peers.getDeviceList().size()));
		for (WifiP2pDevice device : peers.getDeviceList()) {

			Log.d(TAG, "- nazwa: " + device.deviceName + "  adres: " + device.deviceAddress);
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

		// niezaleznie czy wezel jest GO czy nie, musi uruchomic watek
		// odpowiedzialny za nasluchiwanie polaczen. Moze sie przydac w
		// przypadku zerwanego polaczenia
		if (getLocalIpAddress() != null) {
			groupOwnerThread = new GroupOwnerThread(getLocalIpAddress());
			groupOwnerThread.start();
		}
		// String groupOwnerAddress = info.groupOwnerAddress.getHostAddress();
		// czy udalo sie utworzyc grupe z drugim urzadzeniem
		if (info.groupFormed) {
			// jezeli wezel jest wlascicielem grupy to nie robimy nic, bo serwer
			// zostal juz uruchomiony
			if (info.isGroupOwner) {
				sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.TOAST, "Udalo sie polaczyc jako GO, adres to "
						+ info.groupOwnerAddress.getHostAddress()));
				sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.TOAST, "Moj adres IP to  " + getLocalIpAddress()));
			}
			// jezeli nie jest wlascicielem grupy to powinien rozpoczac probe
			// polaczenia sie z drugim wezlem
			else {
				sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.TOAST, "Udalo sie polaczyc jako client, adres to "
						+ info.groupOwnerAddress.getHostAddress()));
				sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.TOAST, "Moj adres IP to  " + getLocalIpAddress()));
				ClientThread clientThread = new ClientThread(info.groupOwnerAddress.getHostAddress());
				clientThread.start();
			}
		} else {
			sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.TOAST, "Nie udalo sie polaczyc jako client"));
		}

	};

	@Override
	public void onGroupInfoAvailable(WifiP2pGroup group) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onChannelDisconnected() {
		// TODO Auto-generated method stub

	}

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
				// socket.setReuseAddress(true);
				while (true) {
					startCommunication(socket.accept());
				}
				// Socket acceptedSocket = socket.accept();
				// startCommunication(acceptedSocket);
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
				socket.connect(new InetSocketAddress(groupOwnerAddress, 4545), 0);
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
		private String macAddress;

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

						if (packet instanceof WifiHelloPacket) {
							WifiHelloPacket helloPacket = (WifiHelloPacket) packet;
							newConnectionEstablished(this, helloPacket.getMacAddress(), socket.getRemoteSocketAddress().toString());
							macAddress = helloPacket.getMacAddress();
						}

						sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.PACKET_RECEIVED, packet));

						Log.d(TAG, "Rec:" + String.valueOf(buffer));

					} catch (IOException e) {
						Log.e(TAG, "disconnected", e);

						break;
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
					handleDisconnect(macAddress);
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
				handleDisconnect(macAddress);
			}
		}

		public void cancel() {
			try {
				// if (!socket.isInputShutdown()){
				// socket.shutdownInput();
				// }
				// if (!socket.isOutputShutdown()){
				// socket.shutdownOutput();
				// }
				if (!socket.isClosed()) {
					socket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public String getAddress() {
			return macAddress;
		}

		public Boolean isValid() {
			return !socket.isClosed();
		}
	}

}

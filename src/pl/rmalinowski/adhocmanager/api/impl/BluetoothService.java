package pl.rmalinowski.adhocmanager.api.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import pl.rmalinowski.adhocmanager.api.PhysicalLayerService;
import pl.rmalinowski.adhocmanager.model.Node;
import pl.rmalinowski.adhocmanager.model.PhysicalLayerEvent;
import pl.rmalinowski.adhocmanager.model.PhysicalLayerEventType;
import pl.rmalinowski.adhocmanager.model.PhysicalLayerState;
import pl.rmalinowski.adhocmanager.model.packets.Packet;
import pl.rmalinowski.adhocmanager.persistence.NodeDao;
import pl.rmalinowski.adhocmanager.utils.SerializationUtils;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class BluetoothService extends PhysicalLayerService {

	private final IBinder mBinder = new MyBinder();
	private static final String TAG = "AodvService";

	private ArrayList<UUID> possibleUuids;
	private static final String APP_NAME = "ahHocManager";
	private Map<String, ActiveConnectionThread> connectedDevices;
	private ConnectionReceiverThread connectionReceiverThread;
	BluetoothAdapter bluetoothAdapter;
	private volatile int activeSearchesNumber;
	private volatile PhysicalLayerState state;
	private NodeDao nodeDao;

	@Override
	public void onCreate() {
		super.onCreate();
		registerBroadcastRecievers();
		initialize();
		nodeDao = new NodeDao(this);
		nodeDao.open();
	}

	@Override
	public void initialize() {
		state = PhysicalLayerState.INITIALIZING;
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (!bluetoothAdapter.isEnabled()) {
			sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.BLUETOOTH_NOT_ENABLED));
		} else {
			connectedDevices = new HashMap<String, ActiveConnectionThread>();
			initializeUUIDList();
			sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.PHYSICAL_LAYER_INITIALIZED));
		}

	}

	private void sendPhysicalBroadcast(PhysicalLayerEvent event) {
		Intent intent = new Intent(PHYSICAL_LAYER_MESSAGE);
		intent.putExtra(PHYSICAL_LAYER_MESSAGE_TYPE, event);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	@Override
	public void sendPacket(Packet packet, String destination) {
		if (connectedDevices.containsKey(destination)) {
			connectedDevices.get(destination).write(packet);
		}
	}

	@Override
	public void connectToNeighbours() {
		Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
		if (pairedDevices.size() > 0) {

			for (BluetoothDevice device : pairedDevices) {
				if (!isCommunicationWithDeviceActive(device)) {
					connectToDevice(device, 1);
					increaseNumberOfActiveSearches();
				}
			}
		} else {
			sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.CONNECTING_TO_NEIGHBOURS_FINISHED));
		}
	}

	private boolean isCommunicationWithDeviceActive(BluetoothDevice device) {
		if (connectedDevices.containsKey(device.getAddress())) {
			return true;
		} else {
			return false;
		}
	}

	private synchronized void connectToDevice(BluetoothDevice blueDevice, int numberOfRetries) {
		ConnnectionMakerThread connnectionMakerThread = new ConnnectionMakerThread(blueDevice, numberOfRetries);
		connnectionMakerThread.start();
	}

	private synchronized void increaseNumberOfActiveSearches() {
		activeSearchesNumber++;
	}

	private synchronized void decreaseNumberOfActiveSearches() {
		activeSearchesNumber--;
		if (activeSearchesNumber == 0) {
			sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.CONNECTING_TO_NEIGHBOURS_FINISHED));
		}
	}

	@Override
	public void getConnectedDevices() {
		// TODO Auto-generated method stub

	}

	@Override
	public void searchForNeighbours() {
		if (bluetoothAdapter.isDiscovering()) {
			bluetoothAdapter.cancelDiscovery();
		}
		bluetoothAdapter.startDiscovery();
	}

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				if (BluetoothDevice.BOND_NONE == device.getBondState()) {
					try {
						Method m = device.getClass().getMethod("createBond", (Class[]) null);
						m.invoke(device, (Object[]) null);
						Log.d(TAG, "Udalo sie powiazac");
					} catch (Exception e) {
						Log.e(TAG, e.getMessage());
						Log.d(TAG, "Nie udalo sie powiazac");
					}
				} else if (BluetoothDevice.BOND_BONDED == device.getBondState()) {
					Log.d(TAG, "Juz powiazane");
				}
				device.getBondState();
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				Log.d(TAG, "Skonczono przeszukiwanie");
			} else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				int prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
				int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);

				if (prevBondState == BluetoothDevice.BOND_BONDING && bondState == BluetoothDevice.BOND_BONDED) {
					if (nodeDao.getByMacAddress(device.getAddress()) == null) {
						nodeDao.create(new Node(device.getAddress(), device.getName()));
					}
					Log.d(TAG, "Wlasnie powiazano");
				}
			} else if (PhysicalLayerService.PHYSICAL_LAYER_MESSAGE.equals(action)) {
				Log.d(TAG, "Skonczono laczenie sie");
				PhysicalLayerEvent event = (PhysicalLayerEvent) intent.getSerializableExtra(PhysicalLayerService.PHYSICAL_LAYER_MESSAGE_TYPE);
				handlePhysicalLayerEvent(event);
			}
		}
	};

	private void handlePhysicalLayerEvent(PhysicalLayerEvent event) {
		switch (event.getEventType()) {
		case CONNECTING_TO_NEIGHBOURS_FINISHED:
			if (PhysicalLayerState.INITIALIZING == state) {
				startListeningThread();
				state = PhysicalLayerState.INITIALIZED;
			}
			break;
		default:
			break;
		}
	}

	private void registerBroadcastRecievers() {

		this.registerReceiver(broadcastReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

		this.registerReceiver(broadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

		this.registerReceiver(broadcastReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));

		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(PhysicalLayerService.PHYSICAL_LAYER_MESSAGE));
	}

	private void startListeningThread() {
		connectionReceiverThread = new ConnectionReceiverThread();
		connectionReceiverThread.start();
	}

	private synchronized void startCommunication(BluetoothSocket bluetoothSocket) {
		String connectedNeighbourAddress = bluetoothSocket.getRemoteDevice().getAddress();
		sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.CONNECTION_TO_NEIGHBOUR_ESTABLISHED, connectedNeighbourAddress));

		ActiveConnectionThread connectionThread = new ActiveConnectionThread(bluetoothSocket);
		connectionThread.start();
		connectedDevices.put(bluetoothSocket.getRemoteDevice().getAddress(), connectionThread);
	}

	private class ConnectionReceiverThread extends Thread {
		BluetoothServerSocket serverSocket = null;
		boolean active;

		public ConnectionReceiverThread() {
			active = true;
		}

		public void run() {
			for (int i = 0; i < possibleUuids.size() && active; i++) {
				try {
					serverSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord(APP_NAME, possibleUuids.get(i));

					// trying to connect
					BluetoothSocket socket = serverSocket.accept();

					serverSocket.close();
					startCommunication(socket);
				} catch (IOException e) {
					Log.e(this.getName(), e.toString());
				}
			}
		}

		public void cancel() {
			try {
				active = false;
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class ActiveConnectionThread extends Thread {
		private final BluetoothSocket blueSocket;
		private final InputStream inStream;
		private final OutputStream outStream;

		public ActiveConnectionThread(BluetoothSocket blueSocket) {
			this.blueSocket = blueSocket;
			InputStream tempInStream = null;
			OutputStream tempOutStream = null;
			try {
				tempInStream = blueSocket.getInputStream();
				tempOutStream = blueSocket.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
			inStream = tempInStream;
			outStream = tempOutStream;
		}

		public void run() {
			byte[] buffer = new byte[1024];
			try {
				while (true) {
					inStream.read(buffer);
					Packet packet = (Packet) SerializationUtils.deserialize(buffer);
					sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.DATA_PACKET_RECEIVED, packet));

				}
			} catch (Exception e) {
				e.printStackTrace();
				sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.CONNECTION_TO_NEIGHBOUR_LOST, blueSocket.getRemoteDevice().getAddress()));
			}
		}

		public void write(Packet packet) {
			try {
				byte[] buffer = SerializationUtils.serialize(packet);
				outStream.write(buffer);
				sendPhysicalBroadcast(new PhysicalLayerEvent(PhysicalLayerEventType.DATA_PACKET_SEND, packet));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void cancel() {
			try {
				blueSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public BluetoothDevice getBluetoothDevice() {
			return blueSocket.getRemoteDevice();
		}
	}

	private class ConnnectionMakerThread extends Thread {
		BluetoothDevice blueDevice;
		private static final int CONNECTION_RETRY_DELAY = 100;
		private boolean active;

		BluetoothSocket blueSocket = null;
		int numberOfRetries = 0;

		public ConnnectionMakerThread(BluetoothDevice blueDevice, int numberOfRetries) {
			this.blueDevice = blueDevice;
			this.numberOfRetries = numberOfRetries;
			active = true;
		}

		public void run() {
			BluetoothSocket connectionSocket = getConnectionSocket();
			if (connectionSocket != null) {

				startCommunication(connectionSocket);
			}
		}

		private BluetoothSocket getConnectionSocket() {
			bluetoothAdapter.cancelDiscovery();

			for (int j = 0; j < numberOfRetries; j++) {
				for (int i = 0; i < possibleUuids.size(); i++) {
					try {
						if (active) {
							blueSocket = blueDevice.createRfcommSocketToServiceRecord(possibleUuids.get(i));
							blueSocket.connect();
						} else {
							break;
						}
						decreaseNumberOfActiveSearches();
						return blueSocket;
					} catch (IOException e) {
						e.printStackTrace();
						try {
							Thread.sleep(CONNECTION_RETRY_DELAY);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
			decreaseNumberOfActiveSearches();
			return null;
		}

		public void cancel() {
			try {
				active = false;
				if (blueSocket != null) {
					blueSocket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void initializeUUIDList() {
		possibleUuids = new ArrayList<UUID>();
		possibleUuids.add(UUID.fromString("503c7430-bc23-11de-8a39-0800200c9a66"));
		possibleUuids.add(UUID.fromString("503c7431-bc23-11de-8a39-0800200c9a66"));
		// possibleUuids.add(UUID.fromString("503c7432-bc23-11de-8a39-0800200c9a66"));
		// possibleUuids.add(UUID.fromString("503c7433-bc23-11de-8a39-0800200c9a66"));
		// possibleUuids.add(UUID.fromString("503c7434-bc23-11de-8a39-0800200c9a66"));
		// possibleUuids.add(UUID.fromString("503c7435-bc23-11de-8a39-0800200c9a66"));
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return Service.START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		nodeDao.close();
		super.onDestroy();
	}

}

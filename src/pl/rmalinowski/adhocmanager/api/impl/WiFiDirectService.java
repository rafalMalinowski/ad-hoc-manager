package pl.rmalinowski.adhocmanager.api.impl;

import java.util.Set;

import pl.rmalinowski.adhocmanager.api.PhysicalLayerService;
import pl.rmalinowski.adhocmanager.model.Node;
import pl.rmalinowski.adhocmanager.model.PhysicalLayerState;
import pl.rmalinowski.adhocmanager.model.packets.Packet;
import pl.rmalinowski.adhocmanager.persistence.NodeDao;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

public class WiFiDirectService extends PhysicalLayerService {

	private final IBinder mBinder = new MyBinder();
	private static final String TAG = "WiFiDirectService";
	private volatile PhysicalLayerState state;
	private NodeDao nodeDao;
//	private WifiP2pManager mManager;
//	private Channel mChannel;

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
//		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
//		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
//		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
//		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

//		this.registerReceiver(broadcastReceiver, mIntentFilter);
	}

	@Override
	public void initialize() {
		state = PhysicalLayerState.INITIALIZING;
//		mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
//		mChannel = (Channel) mManager.initialize(this, getMainLooper(), null);
		Log.d(TAG, "koniec");
	}

	@Override
	public void sendPacket(Packet packet, String destination) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendPacketBroadcast(Packet packet) {
		// TODO Auto-generated method stub

	}

//	@Override
//	public void searchForNeighbours() {
//		mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
//			@Override
//			public void onSuccess() {
//				Log.d(TAG, "udalo sie");
//			}
//
//			@Override
//			public void onFailure(int reasonCode) {
//				Log.d(TAG, "nie udalo sie");
//			}
//		});
//
//	}

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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sendPacketBroadcastExceptOneAddress(Packet packet, String address) {
		// TODO Auto-generated method stub

	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

//	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
//		@Override
//		public void onReceive(Context context, Intent intent) {
//
//			String action = intent.getAction();
//			if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
//				int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
//				if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
//					Log.d(TAG, "jest ok");
//				} else {
//					Log.d(TAG, "srednio :/");
//				}
//
//			} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
//				Log.d(TAG, "test");
//				// Call WifiP2pManager.requestPeers() to get a list of current
//				// peers
//			} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
//				Log.d(TAG, "test");
//				// Respond to new connection or disconnections
//			} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
//				Log.d(TAG, "test");
//				// Respond to this device's wifi state changing
//			}
//
//		}
//	};

	@Override
	public void onDestroy() {
		nodeDao.close();
//		unregisterReceiver(broadcastReceiver);
		super.onDestroy();
	}

	@Override
	public void searchForNeighbours() {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void connectToDevice(String address, int numberOfRetries) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disconnectFromDevice(String address) {
		// TODO Auto-generated method stub
		
	};

}

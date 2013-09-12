package pl.rmalinowski.adhocmanager.api.impl;

import java.io.Serializable;
import java.util.List;

import pl.rmalinowski.adhocmanager.api.NetworkLayerService;
import pl.rmalinowski.adhocmanager.api.PhysicalLayerService;
import pl.rmalinowski.adhocmanager.model.NetworkLayerEvent;
import pl.rmalinowski.adhocmanager.model.NetworkLayerEventType;
import pl.rmalinowski.adhocmanager.model.PhysicalLayerEvent;
import pl.rmalinowski.adhocmanager.model.RoutingTableEntry;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
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

	@Override
	public void onCreate() {
		super.onCreate();
		bindService(new Intent(this, BluetoothService.class), mConnection, Context.BIND_AUTO_CREATE);
		registerBroadcastRecievers();
	}

	@Override
	public void sendData(Serializable data) {
		// TODO Auto-generated method stub

	}

	private void handlePhysicalLayerEvent(PhysicalLayerEvent event) {
		switch (event.getEventType()) {
		case BLUETOOTH_NOT_ENABLED:
			sendNetworkBroadcast(new NetworkLayerEvent(NetworkLayerEventType.ADAPTED_DISABLED, BluetoothAdapter.ACTION_REQUEST_ENABLE));
			break;
		default:
			break;
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
	public String getText() {
		return "OK!";
	}

	@Override
	public void onDestroy() {
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

}

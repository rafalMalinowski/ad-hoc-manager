package pl.rmalinowski.adhocmanager;

import pl.rmalinowski.adhocmanager.api.NetworkLayerService;
import pl.rmalinowski.adhocmanager.api.PhysicalLayerService;
import pl.rmalinowski.adhocmanager.api.impl.AodvService;
import pl.rmalinowski.adhocmanager.events.NetworkLayerEvent;
import pl.rmalinowski.adhocmanager.events.PhysicalLayerEvent;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

public abstract class AbstractAdHocManagerActivity extends Activity {

	protected NetworkLayerService networkService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onPause() {
		unregisterBroadcastRecievers();
		unbindService(mConnection);
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerBroadcastRecievers();
		bindService(new Intent(this, AodvService.class), mConnection, Context.BIND_AUTO_CREATE);
	}

	protected void registerBroadcastRecievers() {
		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(NetworkLayerService.NETWORK_LAYER_MESSAGE));
		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(PhysicalLayerService.PHYSICAL_LAYER_MESSAGE));
	}

	protected void unregisterBroadcastRecievers() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
	}

	protected BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (NetworkLayerService.NETWORK_LAYER_MESSAGE.equals(action)) {
				NetworkLayerEvent event = (NetworkLayerEvent) intent.getSerializableExtra(NetworkLayerService.NETWORK_LAYER_MESSAGE_TYPE);
				handleNetworkLayerEvent(event);
			} else if (PhysicalLayerService.PHYSICAL_LAYER_MESSAGE.equals(action)) {
				PhysicalLayerEvent event = (PhysicalLayerEvent) intent.getSerializableExtra(PhysicalLayerService.PHYSICAL_LAYER_MESSAGE_TYPE);
				handlePhysicalLayerEvent(event);
			}
		}
	};

	protected ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			networkService = (NetworkLayerService) ((NetworkLayerService.MyBinder) binder).getService();
			networkServiceBinded();
		}

		public void onServiceDisconnected(ComponentName className) {
			networkService = null;
		}
	};

	protected abstract void handleNetworkLayerEvent(NetworkLayerEvent event);

	protected void handlePhysicalLayerEvent(PhysicalLayerEvent event) {
	}

	protected void networkServiceBinded() {

	}
}

package pl.rmalinowski.adhocmanager;

import pl.rmalinowski.adhocmanager.api.NetworkLayerService;
import pl.rmalinowski.adhocmanager.api.impl.AodvService;
import pl.rmalinowski.adhocmanager.model.NetworkLayerEvent;
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

public abstract class AbstractAdHocManagerActivity extends Activity{
	

	protected NetworkLayerService networkService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		registerBroadcastRecievers();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		unregisterBroadcastRecievers();
		unbindService(mConnection);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		bindService(new Intent(this, AodvService.class), mConnection, Context.BIND_AUTO_CREATE);
	}
	
	protected void registerBroadcastRecievers(){
		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(NetworkLayerService.NETWORK_LAYER_MESSAGE));
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
			}
		}
	};
	
	protected ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			networkService = (NetworkLayerService) ((NetworkLayerService.MyBinder) binder).getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			networkService = null;
		}
	};
	
	protected abstract void handleNetworkLayerEvent(NetworkLayerEvent event);
}

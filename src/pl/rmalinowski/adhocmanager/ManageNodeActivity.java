package pl.rmalinowski.adhocmanager;

import pl.rmalinowski.adhocmanager.api.PhysicalLayerService;
import pl.rmalinowski.adhocmanager.events.NetworkLayerEvent;
import pl.rmalinowski.adhocmanager.utils.AhHocManagerConfiguration;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ManageNodeActivity extends AbstractAdHocManagerActivity implements OnClickListener {

	public static final String ADDRESS_INTENT_EXTRA = "address";
	private static final String TAG = "AodvService";
	private PhysicalLayerService physicalService;
	private Button connect;
	private Button send;
	private Button disconnect;
	private Button spam;
	private String address;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.manage_node);
		initializeFields();
		setListeners();
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			address = extras.getString(ADDRESS_INTENT_EXTRA);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		bindService(new Intent(this, AhHocManagerConfiguration.physicalLayerClass), physicalConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onPause() {
		unbindService(physicalConnection);
		super.onPause();
	}

	private void initializeFields() {
		connect = (Button) findViewById(R.id.manage_button_connect);
		send = (Button) findViewById(R.id.manage_button_send);
		disconnect = (Button) findViewById(R.id.manage_button_disconnect);
		spam = (Button) findViewById(R.id.manage_button_spam);
	}

	private void setListeners() {
		connect.setOnClickListener(this);
		send.setOnClickListener(this);
		disconnect.setOnClickListener(this);
		spam.setOnClickListener(this);
	}

	@Override
	protected void handleNetworkLayerEvent(NetworkLayerEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.manage_button_connect:
			physicalService.connectToDevice(address, 4);
			Log.d(TAG, "test");
			finish();
			break;
		case R.id.manage_button_send:
			networkService.sendData("test", address);
			finish();
			break;
		case R.id.manage_button_disconnect:
			physicalService.disconnectFromDevice(address);
			finish();
			break;
		case R.id.manage_button_spam:
			new Thread(new Runnable() {
				public void run() {
					int id = 0;
					String addrs = address;
					while (true) {
						Log.d("TAG", "dzialam");
						id++;
						networkService.sendData("test-" + id, addrs);
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							Log.d(TAG, "przerwano watek!");
						}
					}
				}
			}).start();
			finish();
			break;
		default:
			break;
		}
	}

	protected ServiceConnection physicalConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			physicalService = (PhysicalLayerService) ((PhysicalLayerService.MyBinder) binder).getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			physicalService = null;
		}
	};

}

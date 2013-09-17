package pl.rmalinowski.adhocmanager;

import pl.rmalinowski.adhocmanager.api.impl.AodvService;
import pl.rmalinowski.adhocmanager.api.impl.BluetoothService;
import pl.rmalinowski.adhocmanager.events.NetworkLayerEvent;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AbstractAdHocManagerActivity implements OnClickListener {

	private static final int ADAPTED_DISABLED_BROADCAST_EVENT = 1;

	private Button searchButton;
	private Button connectButton;
	private Button sendButton;
	private Button testButton1;
	private Button testButton2;
	private Button testButton3;
	private Button exitButton;
	private Button discoverButton;
	private Button deviceListButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		initializeFields();
		setListeners();
		startServices();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void initializeFields() {
		searchButton = (Button) findViewById(R.id.main_search_for_devices);
		connectButton = (Button) findViewById(R.id.main_connect_button);
		sendButton = (Button) findViewById(R.id.main_send_button);
		testButton1 = (Button) findViewById(R.id.main_test_1);
		testButton2 = (Button) findViewById(R.id.main_test_2);
		testButton3 = (Button) findViewById(R.id.main_test_3);
		exitButton = (Button) findViewById(R.id.main_exit_button);
		discoverButton = (Button) findViewById(R.id.main_discovery);
		deviceListButton = (Button) findViewById(R.id.main_list_devices);
	}

	private void setListeners() {
		searchButton.setOnClickListener(this);
		connectButton.setOnClickListener(this);
		sendButton.setOnClickListener(this);
		testButton1.setOnClickListener(this);
		testButton2.setOnClickListener(this);
		testButton3.setOnClickListener(this);
		exitButton.setOnClickListener(this);
		discoverButton.setOnClickListener(this);
		deviceListButton.setOnClickListener(this);
	}

	private void startServices() {
		startService(new Intent(this, BluetoothService.class));
		// startService(new Intent(this, WiFiDirectService.class));
		startService(new Intent(this, AodvService.class));
	}

	protected void handleNetworkLayerEvent(NetworkLayerEvent event) {
		switch (event.getEventType()) {
		case ADAPTED_DISABLED:
			String actionName = (String) event.getData();
			Intent enableIntent = new Intent(actionName);
			startActivityForResult(enableIntent, ADAPTED_DISABLED_BROADCAST_EVENT);
			break;
		case SHOW_TOAST:
			String textToShow = (String) event.getData();
			Toast.makeText(this, textToShow, Toast.LENGTH_LONG).show();
			break;
		default:
			break;
		}

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.main_exit_button:
			if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
				BluetoothAdapter.getDefaultAdapter().disable();
			}
			finish();
			break;
		case R.id.main_search_for_devices:
			networkService.searchForDevices();
			break;
		case R.id.main_connect_button:
			networkService.connectToNeighbours();
			break;
		case R.id.main_send_button:
			networkService.sendBroadcastData("dziala");
			break;
		case R.id.main_test_1:
			networkService.test1();
			break;
		case R.id.main_test_2:
			networkService.test2();
			break;
		case R.id.main_test_3:
			networkService.test3();
			break;
		case R.id.main_discovery:
			startActivity(new Intent(this, DiscoverActivity.class));
		case R.id.main_list_devices:
			startActivity(new Intent(this, RoutingTableActivity.class));
		default:
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case ADAPTED_DISABLED_BROADCAST_EVENT:
			if (resultCode == Activity.RESULT_OK) {
				// everything is ok
				networkService.reInitialize();
			} else {
				finish();
			}
			break;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.discoverable:
			ensureDiscoverable();
			return true;
		default:
			break;
		}
		return false;
	}

	private void ensureDiscoverable() {
		if (BluetoothAdapter.getDefaultAdapter().getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 5000);
			startActivity(discoverableIntent);
		}
	}

	@Override
	protected void onDestroy() {
		stopService(new Intent(this, AodvService.class));
		super.onDestroy();
	}

}

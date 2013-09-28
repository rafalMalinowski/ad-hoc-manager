package pl.rmalinowski.adhocmanager;

import pl.rmalinowski.adhocmanager.events.NetworkLayerEvent;
import pl.rmalinowski.adhocmanager.events.PhysicalLayerEvent;
import pl.rmalinowski.adhocmanager.utils.AhHocManagerConfiguration;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AbstractAdHocManagerActivity implements OnClickListener {

	private static final int ADAPTED_DISABLED_BROADCAST_EVENT = 1;

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
		exitButton = (Button) findViewById(R.id.main_exit_button);
		discoverButton = (Button) findViewById(R.id.main_discovery);
		deviceListButton = (Button) findViewById(R.id.main_list_devices);
	}

	private void setListeners() {
		exitButton.setOnClickListener(this);
		discoverButton.setOnClickListener(this);
		deviceListButton.setOnClickListener(this);
	}

	@SuppressWarnings("rawtypes")
	private void startServices() {

		// startService(new Intent(this,
		// AhHocManagerConfiguration.physicalLayerClass));

		startService(new Intent(this, AhHocManagerConfiguration.networkLayerClass));
	}

	@Override
	protected void handleNetworkLayerEvent(NetworkLayerEvent event) {
		switch (event.getEventType()) {
		case SHOW_TOAST:
			String textToShow = (String) event.getData();
			Toast.makeText(this, textToShow, Toast.LENGTH_LONG).show();
			break;
		default:
			break;
		}
	}

	@Override
	protected void handlePhysicalLayerEvent(PhysicalLayerEvent event) {
		switch (event.getEventType()) {
		case ADAPTER_NOT_ENABLED:
			String actionName = (String) event.getData();
			Intent enableIntent = new Intent(actionName);
			startActivityForResult(enableIntent, ADAPTED_DISABLED_BROADCAST_EVENT);
			break;
		case TOAST:
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
			stopService(new Intent(this, AhHocManagerConfiguration.networkLayerClass));
			finish();
			break;
		case R.id.main_discovery:
			startActivity(new Intent(this, DiscoverActivity.class));
			break;
		case R.id.main_list_devices:
			startActivity(new Intent(this, RoutingTableActivity.class));
			break;
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
		super.onDestroy();
	}

}

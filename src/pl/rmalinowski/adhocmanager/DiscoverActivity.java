package pl.rmalinowski.adhocmanager;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import pl.rmalinowski.adhocmanager.events.NetworkLayerEvent;
import pl.rmalinowski.adhocmanager.events.PhysicalLayerEvent;

public class DiscoverActivity extends AbstractAdHocManagerActivity implements OnClickListener {

	private Button makeDiscoverableButton;
	private Button searchButton;
	private Button stopButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_discover);
		initializeFields();
		setListeners();
	}

	@Override
	protected void handleNetworkLayerEvent(NetworkLayerEvent event) {
		// TODO Auto-generated method stub

	}
	
	@Override
	protected void handlePhysicalLayerEvent(PhysicalLayerEvent event) {
		switch (event.getEventType()) {
		case TOAST:
			String textToShow = (String) event.getData();
			Toast.makeText(this, textToShow, Toast.LENGTH_LONG).show();
			break;
		default:
			break;
		}
	}

	private void initializeFields() {
		makeDiscoverableButton = (Button) findViewById(R.id.discover_make_discover);
		searchButton = (Button) findViewById(R.id.discover_search);
		stopButton = (Button) findViewById(R.id.discover_stop);
	}

	private void setListeners() {
		makeDiscoverableButton.setOnClickListener(this);
		searchButton.setOnClickListener(this);
		stopButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.discover_make_discover:
			ensureDiscoverable();
			break;
		case R.id.discover_search:
			networkService.searchForDevices();
			break;
		case R.id.discover_stop:
			networkService.stopSearchingForDevices();
			break;
		}
	}

	private void ensureDiscoverable() {
		if (BluetoothAdapter.getDefaultAdapter().getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 5000);
			startActivity(discoverableIntent);
		}
	}
}

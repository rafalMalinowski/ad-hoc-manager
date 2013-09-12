package pl.rmalinowski.adhocmanager;

import pl.rmalinowski.adhocmanager.api.impl.AodvService;
import pl.rmalinowski.adhocmanager.api.impl.BluetoothService;
import pl.rmalinowski.adhocmanager.model.NetworkLayerEvent;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends AbstractAdHocManagerActivity implements OnClickListener {

	private static final int ADAPTED_DISABLED_BROADCAST_EVENT = 1;

	private Button searchButton;
	private Button bluetoothButton;
	private Button connectButton;
	private Button exitButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
		bluetoothButton = (Button) findViewById(R.id.main_button_bluetooth);
		connectButton = (Button) findViewById(R.id.main_connect_button);
		exitButton = (Button) findViewById(R.id.main_exit_button);
	}

	private void setListeners() {
		searchButton.setOnClickListener(this);
		bluetoothButton.setOnClickListener(this);
		connectButton.setOnClickListener(this);
		exitButton.setOnClickListener(this);
	}

	private void startServices() {
		startService(new Intent(this, AodvService.class));
		startService(new Intent(this, BluetoothService.class));
	}

	protected void handleNetworkLayerEvent(NetworkLayerEvent event) {
		switch (event.getEventType()) {
		case ADAPTED_DISABLED:
			String actionName = (String) event.getData();
			Intent enableIntent = new Intent(actionName);
			startActivityForResult(enableIntent, ADAPTED_DISABLED_BROADCAST_EVENT);
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
		case R.id.main_button_bluetooth:
			startActivity(new Intent(this, BluetoothActivity.class));
			break;
		case R.id.main_search_for_devices:
			networkService.searchForDevices();
		case R.id.main_connect_button:
			networkService.connectToNeighbours();
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

}
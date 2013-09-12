package pl.rmalinowski.adhocmanager;

import java.util.Set;

import pl.rmalinowski.adhocmanager.api.NetworkLayerService;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class BluetoothActivity extends Activity implements OnClickListener {

	private Button startButton;
	private Button findDevicesButton;
	BluetoothAdapter bluetoothAdapter;
	private NetworkLayerService networkService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth);
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		initializeFields();
		setListeners();
//		networkService = NetworkLayerService.
		registerBraodcastRecievers();
	}

	private void setListeners() {
		startButton.setOnClickListener(this);
		findDevicesButton.setOnClickListener(this);
	}

	private void initializeFields() {
		startButton = (Button) findViewById(R.id.bluetooth_button_start);
		findDevicesButton = (Button) findViewById(R.id.bluetooth_button_find_devices);

	}


	private void findDevices() {
		
		setProgressBarIndeterminateVisibility(true);

		if (bluetoothAdapter.isDiscovering()) {
			bluetoothAdapter.cancelDiscovery();
		}

		bluetoothAdapter.startDiscovery();
//		bluetoothAdapter.
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.bluetooth_button_start:
			if(!bluetoothAdapter.isEnabled()){
				bluetoothAdapter.enable();
			}
			break;
		case R.id.bluetooth_button_find_devices:
			findDevices();
			break;
		default:
			break;
		}
	}

	private void registerBraodcastRecievers() {
		// Register for broadcasts when device is found
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(receiver, filter);

		// Register for broadcasts when discovery has finished
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(receiver, filter);
	}
	
	private final BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Set<BluetoothDevice> devices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				
				
				try {
//					Method m = device.getClass().getMethod("createBond", (Class[]) null);
//				     m.invoke(device, (Object[]) null);

//					Class c = Class.forName("android.bluetooth.BluetoothAdapter");
//					Method m = c.getMethod("getUuids", new Class[] {});
//					Object ret = m.invoke(c);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {

				}
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				setProgressBarIndeterminateVisibility(false);
				
			} else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
				// getPairedDevices();
			}
		}
	};

}

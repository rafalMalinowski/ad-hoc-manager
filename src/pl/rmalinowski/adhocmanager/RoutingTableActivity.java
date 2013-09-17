package pl.rmalinowski.adhocmanager;

import java.util.Set;

import pl.rmalinowski.adhocmanager.events.NetworkLayerEvent;
import pl.rmalinowski.adhocmanager.model.RoutingTableEntry;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class RoutingTableActivity extends AbstractAdHocManagerActivity {

	private Set<RoutingTableEntry> routingTable;
	private static final Integer ADDRESS_IN_TABLE_POSITON = 2;
	private boolean initialized = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_routing_table);
	}

	@Override
	protected void networkServiceBinded() {
		routingTable = networkService.getRoutingTable();
		if (!initialized) {
			initialize();
		}
	}

	protected void handleNetworkLayerEvent(NetworkLayerEvent event) {
		switch (event.getEventType()) {
		case SHOW_TOAST:
			String textToShow = (String) event.getData();
			Toast.makeText(this, textToShow, Toast.LENGTH_LONG).show();
			break;
		case DATA_RECIEVED:
			textToShow = (String) event.getData();
			Toast.makeText(this, textToShow, Toast.LENGTH_LONG).show();
			break;
		default:
			break;
		}
	}

	private void initialize() {
		final TableLayout table = (TableLayout) findViewById(R.id.routing_table);
		for (RoutingTableEntry entry : routingTable) {
			final TableRow tr = (TableRow) getLayoutInflater().inflate(R.layout.routing_table_row, null);

			TextView tv;
			tv = (TextView) tr.findViewById(R.id.cell_id);
			tv.setText(String.valueOf(entry.getDestinationNode().getId()));

			TextView tv2;
			tv2 = (TextView) tr.findViewById(R.id.cell_name);
			tv2.setText(entry.getDestinationNode().getName());

			TextView tv3;
			tv3 = (TextView) tr.findViewById(R.id.cell_address);
			tv3.setText(entry.getDestinationNode().getAddress());

			TextView tv4;
			tv4 = (TextView) tr.findViewById(R.id.cell_nextHop);
			if (entry.getNextHopAddress() != null) {
				tv4.setText(entry.getNextHopAddress());
			} else {
				tv4.setText("---");
			}

			TextView tv5;
			tv5 = (TextView) tr.findViewById(R.id.cell_hopCount);
			if (entry.getHopCount() != null) {
				tv5.setText(entry.getHopCount());
			} else {

				tv5.setText("---");
			}

			TextView tv6;
			tv6 = (TextView) tr.findViewById(R.id.cell_status);
			tv6.setText(entry.getState().toString());

			tr.setOnLongClickListener(new OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {
					TableRow tablerow = (TableRow) v;
					TextView textView = (TextView) tablerow.getChildAt(ADDRESS_IN_TABLE_POSITON);
					Intent intent = new Intent(getBaseContext(), ManageNodeActivity.class);
					intent.putExtra(ManageNodeActivity.ADDRESS_INTENT_EXTRA, textView.getText());
					startActivity(intent);
					return true;
				}
			});
			table.addView(tr);
			initialized = true;
		}
	}
}

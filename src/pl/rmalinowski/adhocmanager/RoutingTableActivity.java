package pl.rmalinowski.adhocmanager;

import java.util.Date;
import java.util.Set;

import pl.rmalinowski.adhocmanager.events.NetworkLayerEvent;
import pl.rmalinowski.adhocmanager.model.RoutingTableEntry;
import pl.rmalinowski.adhocmanager.model.RoutingTableEntryState;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
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
			initialize();
			break;
		case DATA_RECIEVED:
			textToShow = (String) event.getData();
//			Toast.makeText(this, textToShow, Toast.LENGTH_SHORT).show();
			initialize();
			break;
		case NETWORK_STATE_CHANGED:
			initialize();
			break;
		case DESTINATION_UNREACHABLE:
			Toast.makeText(this, "nie udalo sie wyslanie wiadomosci", Toast.LENGTH_LONG).show();
			break;
		default:
			break;
		}
	}

	private void initialize() {
		final TableLayout table = (TableLayout) findViewById(R.id.routing_table);
		table.removeAllViews();
		table.addView(getFirstRow());
		for (RoutingTableEntry entry : routingTable) {
			final TableRow tr = (TableRow) getLayoutInflater().inflate(R.layout.routing_table_row, null);

			TextView tv = (TextView) tr.findViewById(R.id.cell_id);
			tv.setText(String.valueOf(entry.getDestinationNode().getId()));

			tv = (TextView) tr.findViewById(R.id.cell_name);
			tv.setText(entry.getDestinationNode().getName());

			tv = (TextView) tr.findViewById(R.id.cell_address);
			tv.setText(entry.getDestinationNode().getAddress());

			tv = (TextView) tr.findViewById(R.id.cell_nextHop);
			if (entry.getNextHopAddress() != null && RoutingTableEntryState.VALID == entry.getState()) {
				tv.setText(entry.getNextHopAddress());
			} else {
				tv.setText("---");
			}

			tv = (TextView) tr.findViewById(R.id.cell_hopCount);
			if (entry.getHopCount() != null && RoutingTableEntryState.VALID == entry.getState()) {
				tv.setText(entry.getHopCount().toString());
			} else {
				tv.setText("---");
			}

			tv = (TextView) tr.findViewById(R.id.cell_status);
			tv.setText(entry.getState().toString());
			int color = 0;
			switch (entry.getState()) {
			case INVALID:
				color = Color.parseColor("#E80909");
				break;
			case VALID:
				color = Color.parseColor("#23E809");
				break;
			case VALIDATING:
				color = Color.parseColor("#E6ED09");
				break;
			default:
				break;
			}
			tv.setTextColor(color);

			tv = (TextView) tr.findViewById(R.id.cell_lifetime);

			if (entry.getValidTimestamp() != null && RoutingTableEntryState.VALID == entry.getState()) {
				long lifeTimeInSeconds;
				if ((entry.getValidTimestamp() - new Date().getTime()) > 0) {
					lifeTimeInSeconds = (entry.getValidTimestamp() - new Date().getTime()) / 1000;
				} else {
					lifeTimeInSeconds = 0;
				}
				tv.setText(String.valueOf(lifeTimeInSeconds));
			} else {
				tv.setText("---");
			}
			tr.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					TableRow tablerow = (TableRow) v;
					TextView textView = (TextView) tablerow.getChildAt(ADDRESS_IN_TABLE_POSITON);
					Intent intent = new Intent(getBaseContext(), ManageNodeActivity.class);
					intent.putExtra(ManageNodeActivity.ADDRESS_INTENT_EXTRA, textView.getText());
					startActivity(intent);
				}
			});
			// tr.setOnLongClickListener(new OnLongClickListener() {
			//
			// @Override
			// public boolean onLongClick(View v) {
			// TableRow tablerow = (TableRow) v;
			// TextView textView = (TextView)
			// tablerow.getChildAt(ADDRESS_IN_TABLE_POSITON);
			// Intent intent = new Intent(getBaseContext(),
			// ManageNodeActivity.class);
			// intent.putExtra(ManageNodeActivity.ADDRESS_INTENT_EXTRA,
			// textView.getText());
			// startActivity(intent);
			// return true;
			// }
			// });
			table.addView(tr);
		}
		initialized = true;
	}

	private TableRow getFirstRow() {
		final TableRow tr = (TableRow) getLayoutInflater().inflate(R.layout.routing_table_row, null);
		TextView tv = (TextView) tr.findViewById(R.id.cell_id);
		tv.setText("ID");
		tv.setTypeface(null, Typeface.BOLD);

		tv = (TextView) tr.findViewById(R.id.cell_name);
		tv.setText("NAME");
		tv.setTypeface(null, Typeface.BOLD);

		tv = (TextView) tr.findViewById(R.id.cell_address);
		tv.setText("ADDRESS");
		tv.setTypeface(null, Typeface.BOLD);

		tv = (TextView) tr.findViewById(R.id.cell_nextHop);
		tv.setText("NEXT HOP");
		tv.setTypeface(null, Typeface.BOLD);

		tv = (TextView) tr.findViewById(R.id.cell_hopCount);
		tv.setText("HOPS");
		tv.setTypeface(null, Typeface.BOLD);

		tv = (TextView) tr.findViewById(R.id.cell_status);
		tv.setText("STATUS");
		tv.setTypeface(null, Typeface.BOLD);

		tv = (TextView) tr.findViewById(R.id.cell_lifetime);
		tv.setText("LIFETIME");
		tv.setTypeface(null, Typeface.BOLD);
		return tr;
	}
}

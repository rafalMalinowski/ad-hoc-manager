package pl.rmalinowski.adhocmanager.persistence;

import java.util.HashSet;
import java.util.Set;

import pl.rmalinowski.adhocmanager.model.Node;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class NodeDao extends AbstractDbAdapter {

	public static final String COLUMN_MAC_ADDRESS = "macAddress";
	public static final String COLUMN_DEVICE_NAME = "deviceName";
	public static final String COLUMN_ID = "_id";
	public static final String[] TABLE_COLUMNS = new String[] { COLUMN_ID, COLUMN_DEVICE_NAME, COLUMN_MAC_ADDRESS };

	public static final String TABLE_NAME = "nodes";

	public NodeDao(Context contex) {
		super(contex);
	}

	public long create(Node n) {
		ContentValues contVal = new ContentValues();
		contVal.put(COLUMN_MAC_ADDRESS, n.getAddress());
		contVal.put(COLUMN_DEVICE_NAME, n.getName());

		return db.insert(TABLE_NAME, null, contVal);
	}

	public boolean delete(long id) {
		return db.delete(TABLE_NAME, COLUMN_ID + "=" + id, null) > 0;
	}

	public Cursor getAll() {
		return db.query(TABLE_NAME, TABLE_COLUMNS, null, null, null, null, null);
	}
	
	public Set<Node> getAllNodes(){
		Set<Node> result = new HashSet<Node>();
		Cursor cursor = getAll();
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			result.add(cursorToNodes(cursor));
			while(!cursor.isLast()){
				cursor.moveToNext();
				result.add(cursorToNodes(cursor));
			}
		}
		return result;
	}

	public Node getById(long id) {
		Cursor cursor = db.query(TABLE_NAME, TABLE_COLUMNS, COLUMN_ID + "=" + id, null, null, null, null);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			return cursorToNodes(cursor);
		} else
			return null;
	}

	public Node getByMacAddress(String macAddress) {
		Cursor cursor = db.query(TABLE_NAME, TABLE_COLUMNS, COLUMN_MAC_ADDRESS + "= '" + macAddress + "'", null, null, null, null);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			return cursorToNodes(cursor);
		} else
			return null;
	}

	public boolean updateNode(Node node) {
		try {
			ContentValues args = new ContentValues();
			args.put(COLUMN_MAC_ADDRESS, node.getAddress());
			args.put(COLUMN_DEVICE_NAME, node.getName());
			db.update(TABLE_NAME, args, COLUMN_ID + "= " + node.getId(), null);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public Node cursorToNodes(Cursor cursor) {
		Node node = new Node();
		node.setId(cursor.getLong(0));
		node.setName(cursor.getString(1));
		node.setAddress(cursor.getString(2));
		return node;
	}
}

package pl.rmalinowski.adhocmanager.persistence;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AbstractDbAdapter {

	protected DatabaseHelper dbHelper;
    protected SQLiteDatabase db;
    

    protected static final String TABLE_CREATE_NODES = "create table nodes (_id integer primary key autoincrement, "
            + "macAddress text not null, deviceName text not null);";
    
    protected static final String DATABASE_NAME = "adHocManagerDb";
    protected static final int DATABASE_VERSION = 1;
    
    protected final Context contex;
    
    public AbstractDbAdapter(Context contex) {
        this.contex = contex;
    }
    
    public AbstractDbAdapter open() throws SQLException {
    	dbHelper = new DatabaseHelper(contex);
        db = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
    	dbHelper.close();
    }
    
    protected static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(TABLE_CREATE_NODES);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS nodes");
            onCreate(db);
        }
    }
}

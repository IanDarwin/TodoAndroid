package todomore.android;

import com.darwinsys.todo.model.Task;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class TaskDao {
	
	private static final String TABLE_TODO = "todo";
	SQLiteDatabase db;
	
	void init(Context context) {
		db = new DbHelper(context, TABLE_TODO, null, 0).getWritableDatabase();
	}
	
	void shutdown() {
		db.close();
	}
	
	/** Inserts a new object */
	void insert(Task t) {
		ContentValues cv = GruntWork.taskToContentValues(t);
		if (db.insert(TABLE_TODO, "_id", cv) != 1) {
			throw new RuntimeException("Insert failed!");
		}
	}

	class DbHelper extends SQLiteOpenHelper {

		public DbHelper(Context context, String name, CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("create table Todo("
					+ "_id integer primary key,"	// PKey in Android SQLite database
					+ "id long integer,"			// PKey in remote database
					+ "name varchar," 
					+ "priority integer,"	// 0 = top, 1, 2, 3 = lowest
					+ "modified integer"
					+ ")"
					);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			throw new UnsupportedOperationException("No upgrades yet!");
		}
		
	}
}

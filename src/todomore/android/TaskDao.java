package todomore.android;

import java.util.List;

import com.darwinsys.todo.model.Task;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TaskDao {
	
	private static final String TAG = "TaskDao";

	Context context;
	private static final String TABLE_TODO = "todo";
	SQLiteDatabase db;
	
	TaskDao(Context context) {
		this.context = context;
		db = new DbHelper(context, "todo.db", null, 1).getWritableDatabase();
	}
	
	void shutdown() {
		db.close();
	}
	
	/** C: Inserts a new object */
	void insert(Task t) {
		ContentValues cv = GruntWork.taskToContentValues(t);
		if (db.insert(TABLE_TODO, "_id", cv) != 1) {
			throw new RuntimeException("Insert failed!");
		}
	}
	
	/** R: Find by id */
	Task findById(long id) {
		Cursor c = db.query(TABLE_TODO, null, "_id = ?", new String[]{Long.toString(id)}, null, null, null);
		c.moveToFirst();
		return GruntWork.cursorToTask(c);
	}
	
	/** R: Find All */
	List<Task> findAll() {
		Cursor c = db.query(TABLE_TODO, null, null, null, null, null, null);
		return GruntWork.cursorToTasks(c);
	}

	class DbHelper extends SQLiteOpenHelper {

		public DbHelper(Context context, String name, CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d(TAG, "onCreate()");
			db.execSQL("create table Todo("
					+ "_id integer primary key,"	// PKey in Android SQLite database
					+ "id long integer,"			// PKey in remote database
					+ "name varchar," 				// Short description of task
					+ "priority integer,"			// 0 = top, 1, 2, 3 = lowest
					+ "modified integer"			// currentTimeMillis when last modified
					+ ")"
					);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			throw new UnsupportedOperationException("No upgrades yet!");
		}
		
	}
}

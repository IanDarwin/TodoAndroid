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
	
	private static final String TAG = "TodoMore.TaskDao";

	Context context;
	private static final String TABLE_TODO = "todo";
	private String _ID = "_ID";
	SQLiteDatabase db;
	
	TaskDao(Context context) {
		this.context = context;
		db = new DbHelper(context, "todo.db", null, 1).getWritableDatabase();
	}
	
	void shutdown() {
		db.close();
	}
	
	/** C: Inserts a new object */
	long insert(Task t) {
		ContentValues cv = GruntWork.taskToContentValuesWithout_ID(t);
		Log.d(TAG, "Inserting task " + t);
		Long _id = cv.getAsLong(_ID);
		if (_id != null) {
			if (_id != 0L) {
				throw new IllegalArgumentException("Trying to insert Task with _id = " + _id);
			}
		}
		long newId = db.insert(TABLE_TODO, "name", cv);
		if (newId == -1) {
			throw new RuntimeException("Insert failed!");
		}
		return newId;
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
		return GruntWork.cursorToTaskList(c);
	}
	
	/** U: Update */
	public boolean update(Task t) {
		if (!(t instanceof AndroidTask)) {
			throw new RuntimeException("Update but Task has no _id!");
		}
		long _id = ((AndroidTask) t).get_Id();
		int rc = db.update(TABLE_TODO, GruntWork.taskToContentValues(t), "_id = ?", new String[]{Long.toString(_id)});
		if (!(rc == 1)) {
			Log.d(TAG, "Warning: Update Failed!");
		}
		return rc == 1;
	}
	
	/** D: Delete */
	public boolean delete(Task t) {
		throw new UnsupportedOperationException("Delete not supported yet");
	}

	class DbHelper extends SQLiteOpenHelper {

		public DbHelper(Context context, String name, CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d(TAG, "onCreate()");
			db.execSQL("create table " + TABLE_TODO + "("
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

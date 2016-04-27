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

	private static final String TABLE_TODO = "todo";
	private static final String TABLE_DELQ = "delete_q";

	private String _ID = "_ID";
	SQLiteDatabase db;
	
	public TaskDao(Context context) {
		db = new DbHelper(context, "todo.db", null, DbHelper.VER_ONE).getWritableDatabase();
		Cursor c = db.query(TABLE_TODO, null, null, null, null, null, null);
		int n = c.getCount();
		Log.d(TAG, "Database starts with " + n + " Tasks");
	}
	
	void shutdown() {
		db.close();
	}
	
	/** C: Inserts a new object */
	public long insert(Task t) {
		t.setModified(System.currentTimeMillis());
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
		t.setDeviceId(newId);
		return newId;
	}
	
	/** R: Find by id */
	Task findById(long id) {
		Cursor c = db.query(TABLE_TODO, null, "_id = ?", new String[]{Long.toString(id)}, null, null, null);
		c.moveToFirst();
		return GruntWork.cursorToTask(c);
	}
	
	/** R: Find All */
	public List<Task> findAll() {
		Cursor c = db.query(TABLE_TODO, null, null, null, null, null, "priority asc, name asc");
		return GruntWork.cursorToTaskList(c);
	}
	
	/** U: Update
	 * @param t The Task to be updated; must already exist locally.
	 * @return True iff the update succeeded
	 */
	public boolean update(Task t) {

		if (t.getDeviceId() == null) {
			throw new IllegalArgumentException(
				"TaskDao: update on non-local task: " + t);
		}
		long _id = t.getDeviceId();
		t.setModified(System.currentTimeMillis());
		int rc = db.update(TABLE_TODO, GruntWork.taskToContentValues(t), "_id = ?", new String[]{Long.toString(_id)});
		return rc == 1;
	}
	
	/** D: Delete */
	public boolean delete(Task t) {
		String taskIdString = Long.toString(t.getDeviceId());
		int deleted = db.delete(TABLE_TODO, "_id = ?", new String[]{taskIdString});
		Log.d(TAG, "TaskDao.delete(" + taskIdString + ") --> " + deleted);
		if (t.getServerId() > 0) {
			db.execSQL(String.format("insert into " + TABLE_DELQ + "(remoteId) values(%d)", t.getServerId()));
		}
		return true;
	}

	class DbHelper extends SQLiteOpenHelper {

		public final static int VER_ZERO = 1,
				VER_ONE = 2;
		public DbHelper(Context context, String name, CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d(TAG, "onCreate()");
			db.execSQL("create table " + TABLE_TODO + "("
					+ "_id integer primary key,"	// PKey in Android SQLite database
					+ "server_id long integer,"		// PKey in remote database
					+ "name varchar," 				// Short description of task
					+ "description varchar,"		// Longer description
					+ "priority integer,"			// 0 = top, 1, 2, 3 = lowest
					+ "status integer,"
					+ "modified long integer,"			// currentTimeMillis when last modified
					+ "creationdate date,"
					+ "duedate date,"
					+ "completeddate date"
					+ ")"
					);
			db.execSQL("create table " + TABLE_DELQ + "("
					+ "_id integer primary key,"
					+ "remoteId string"
					+ ")"
					);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (oldVersion == VER_ZERO && newVersion == VER_ONE) {
				db.execSQL("alter table " + TABLE_TODO + 
					" add column server_id integer");
				return;
			}
			throw new UnsupportedOperationException(
				"Upgrade from %d to %d not written yet!");
		}
		
	}

}

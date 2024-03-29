package todomore.android;

import java.time.LocalDate;
import java.util.List;

import com.darwinsys.todo.model.Status;
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
		t.setModified(LocalDate.now());
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
		Cursor c = db.query(TABLE_TODO, TASK_COLUMNS, null, null, null, null, "priority asc, name asc");
		return GruntWork.cursorToTaskList(c);
	}
	
	public List<Task> findUncompleted() {
		String select = "status < " + Status.COMPLETE.ordinal();
		Cursor c = db.query(TABLE_TODO, TASK_COLUMNS, select, null, null, null, "priority asc, name asc");
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
		t.setModified(LocalDate.now());
		int rc = db.update(TABLE_TODO, GruntWork.taskToContentValues(t), "_id = ?", new String[]{Long.toString(_id)});
		return rc == 1;
	}
	
	/** D: Delete */
	public boolean delete(Task t) {
		String taskIdString = Long.toString(t.getDeviceId());
		int deleted = db.delete(TABLE_TODO, "_id = ?", new String[]{taskIdString});
		Log.d(TAG, "TaskDao.delete(" + taskIdString + ") --> " + deleted);
		// If it made it to the server, queue it for deletion from there too
		final long serverId = t.getServerId();
		if (serverId > 0) {
			Log.d(TAG, "TaskDao.delete: queuing remote delete " + serverId);
			db.execSQL(String.format("insert into " + TABLE_DELQ + "(remoteId) values(%d)", serverId));
		}
		return true;
	}
	
	/** Used only by the SyncAdapter, to delete remote entries
	 * that got deleted here.
	 * @return An array of the serverIds of tasks deleted locally
	 */
	public long[] findDeletions() {
		Cursor c = db.query(TABLE_DELQ, null, null, null, null, null, null);
		int n = c.getCount();
		long[] ret = new long[n];
		while (c.moveToNext()) {
			ret[--n] = c.getLong(1);
		}
		Log.d(TAG, "TaskDao.findDeletions: " + ret.length + " entries.");
		return ret;
	}
	
	/**
	 * After the Sync Adapter deletes it remotely, remove it from the queue
	 * @author ian
	 */
	public void deleteDeletion(long id) {
		db.delete(TABLE_DELQ, "remoteId = ?", new String[]{Long.toString(id)});
	}

	/** THIS MUST BE KEPT IN SYNC WITH THE CREATE IN ONCREATE BELOW!! */
	public static final String[] TASK_COLUMNS = {
			"_id",				// 0 PKey in Android SQLite database
			"server_id",		// 1 PKey in remote database
			"name", 			// 2 Short description of task
			"description",		// 3 Longer description
			"priority",			// 4 prio 0 = top, 1, 2, 3 = lowest
			"status",			// 5
			"modified",			// 6 LocalDateTime when last modified
			"creationdate",		// 7
			"duedate",			// 8
			"completeddate",	// 9
	};

	class DbHelper extends SQLiteOpenHelper {

		public final static int VER_ZERO = 1,
				VER_ONE = 2;
		public DbHelper(Context context, String name, CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d(TAG, "onCreate()");

			// THIS MUST BE KEPT IN SYNC WITH TASK_COLUMNS ABOVE
			db.execSQL("create table " + TABLE_TODO + "("
					+ "_id integer primary key,"	// PKey in Android SQLite database
					+ "server_id long,"		// PKey in remote database
					+ "name varchar," 				// Short description of task
					+ "description varchar,"		// Longer description
					+ "priority integer,"			// 0 = top, 1, 2, 3 = lowest
					+ "status integer,"
					+ "modified date,"				// datetime when last modified
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

package todomore.android;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.darwinsys.todo.model.Priority;
import com.darwinsys.todo.model.Status;
import com.darwinsys.todo.model.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 
 * This class encapsulates various make-work functions that 
 * WOULD NOT BE NEEDED IF ANDROID'S DB API WERE OBJECT ORIENTED!
 * Should revamp this app to use Rooms. "Someday."
 * @author Ian Darwin
 */
public class GruntWork {
	
	private final static String TAG = "GruntWork";

	public static ContentValues taskToContentValuesWithout_ID(Task t) {
		return taskToContentValues(t, false);
	}

	public static ContentValues taskToContentValues(Task t) {
		return taskToContentValues(t, true);
	}
	
	private static ContentValues taskToContentValues(Task t, boolean include_ID) {
		Log.d(TAG, "taskToContentValues(" + t + ")");
		ContentValues cv = new ContentValues();
		if (t.getDeviceId() != null)
			cv.put("_id", t.getDeviceId());
		cv.put("server_id", t.getServerId());
		cv.put("name", t.getName());
		cv.put("description", t.getDescription());
		Priority priority = t.getPriority();
		if (priority != null) {
			cv.put("priority", priority.ordinal());
		}
		Status status = t.getStatus();
		if (status != null) {
			cv.put("status", status.ordinal());
		}
		LocalDate creationDate = t.getCreationDate();
		if (creationDate != null)
			cv.put("creationDate", creationDate.toString());	// when you decided you had to do it
		LocalDate dueDate = t.getDueDate();
		if (dueDate != null)
			cv.put("dueDate", dueDate.toString());				// when you hoped to do it by
		LocalDate completedDate = t.getCompletedDate();
		if (completedDate != null)
			cv.put("completedDate", completedDate.toString());	// when you actually did it by
		LocalDate modifiedDate = t.getModified();
		if (modifiedDate != null)
			cv.put("modified", t.getModified().toString());		// When you last worked on the entry
		// XXX Project project;	// what this task is part of
		// XXX Context context;	// where to do it
		return cv;
	}

	/** Get the columns from the current row, make into a Task.
	 * N.B. Column numbers are taken from TASK_COLUMNS
	 * @param c
	 * @return
	 */
	public static Task cursorToTask(Cursor c) {
		Log.d(TAG, "cursortToTask");
		if (c.isAfterLast()) {
			Log.d(TAG, "Cursor has no more rows");
			return null;
		}
		Task t = new Task();
		for (int i = 0; i < 7; i++) {
			Log.d(TAG, "i + \", \" + c.getColumnName(i) = " + i + ", " + c.getColumnName(i));
		}
		t.setDeviceId(c.getLong(0));// our idea of pkey
		t.setServerId(c.getInt(1));	// remote's idea of pkey
		t.setName(c.getString(2));
		t.setDescription(c.getString(3));
		t.setPriority(Priority.values()[c.getInt(4)]);
		t.setStatus(Status.values()[c.getInt(5)]);
		String modDateString = c.getString(6);
		if (modDateString != null)
			t.setModified(LocalDate.parse(modDateString));
		String creationDateString = c.getString(7);
		if (creationDateString != null)
			t.setCreationDate(LocalDate.parse(creationDateString));
		String dueDateString = c.getString(8);
		if (dueDateString != null)
			t.setDueDate(LocalDate.parse(dueDateString));
		String completedDateString = c.getString(9);
		if (completedDateString != null)
			t.setCompletedDate(LocalDate.parse(completedDateString));
		// XXX Project project;	// what this task is part of
		// XXX Context context;	// where the task applies
		return t;
	}
	
	public static List<Task> cursorToTaskList(Cursor c) {
		List<Task> list = new ArrayList<Task>();
		while (c.moveToNext()) {
			list.add(cursorToTask(c));
		}
		return list;
	}

	/**
	 * Convert JSON goo like this:
	 * [{"serverId":102,"priority":"High","name":"TEST 2",
	 * "creationDate":{"year":2015,"month":11,"day":1},
	 * "project":null,"context":{"id":100,"name":"Life"},
	 * "dueDate":null,"status":"ACTIVE","completedDate":null,
	 * "modified":1448292432791,"description":"None","complete":false},
	 * {"serverId":103,"priority":"Low","name":"Low prio item",
	 * "creationDate":{"year":2015,"month":10,"day":23},"project":null,
	 * "context":{"id":80,"name":"Home"},"dueDate":null,"status":"NEW",
	 * "completedDate":null,"modified":1448312330383,
	 * "description":"","complete":false}]
	 * into a proper List<Task>.
	 */
	public static List<Task> jsonStringToListTask(String resultStr) {
		List<Task> ret = new ArrayList<Task>();
		try {
			JSONArray array = (JSONArray) new JSONTokener(resultStr).nextValue();
			for (int i = 0; i < array.length(); i++) {
				JSONObject o = array.getJSONObject(i);
				Task t = new Task();
				t.setServerId(o.getLong("serverId"));
				t.setPriority(Priority.valueOf(o.getString("priority")));
				t.setStatus(Status.valueOf(o.getString("status")));
				t.setName(o.getString("name"));
				t.setDescription(o.getString("description"));
				String creationDateString = o.getString("creationDate");
				if (creationDateString != null && !"null".equals(creationDateString))
					t.setCreationDate(LocalDate.parse(creationDateString));
				String modDateString = o.getString("modified");
				if (modDateString != null && !"null".equals(modDateString))
					t.setModified(LocalDate.parse(modDateString));
				String dueDateString = o.getString("dueDate");
				if (dueDateString != null && !"null".equals(dueDateString))
					t.setDueDate(LocalDate.parse(dueDateString));
				String completedDateString = o.getString("completedDate");
				if (completedDateString != null && !"null".equals(completedDateString))
					t.setCompletedDate(LocalDate.parse(completedDateString));
				// XXX Project project;	// what this task is part of
				// XXX Context context;	// where to do it
				ret.add(t);
			}
		} catch (JSONException e) {
			throw new RuntimeException("This JSON failed to parse! " + e, e);
		}
		return ret;
	}
}


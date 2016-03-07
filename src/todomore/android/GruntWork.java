package todomore.android;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.darwinsys.todo.model.Date;
import com.darwinsys.todo.model.Priority;
import com.darwinsys.todo.model.Status;
import com.darwinsys.todo.model.Task;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

/** 
 * This class encapsulates various make-work functions that 
 * WOULD NOT BE NEEDED IF ANDROID'S API WERE OBJECT ORIENTED!
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
		Log.d(TAG, "taskToConentValues(" + t + ")");
		ContentValues cv = new ContentValues();
		cv.put("id", t.getId());
		if (include_ID && t instanceof AndroidTask) {
			cv.put("_id", ((AndroidTask)t).get_Id());
		}
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
		Date creationDate = t.getCreationDate();
		if (creationDate != null)
		cv.put("creationDate", creationDate.toString());		// when you decided you had to do it
			Date dueDate = t.getDueDate();
		if (dueDate != null)
			cv.put("dueDate", dueDate.toString());				// when you hoped to do it by
		Date completedDate = t.getCompletedDate();
		if (completedDate != null)
			cv.put("completedDate", completedDate.toString());	// when you actually did it by
		cv.put("modified", t.getModified());
		// XXX Project project;	// what this task is part of
		// XXX Context context;	// where to do it
		return cv;
	}
 
	public static AndroidTask cursorToTask(Cursor c) {
		if (c.isAfterLast()) {
			Log.d(TAG, "Cursor has no more rows");
			return null;
		}
		AndroidTask t = new AndroidTask();
		// sdumpCursor(c);
		t.set_Id(c.getInt(c.getColumnIndex("_id")));// our idea of pkey
		t.setId(c.getInt(c.getColumnIndex("id")));	// remote's idea of pkey
		t.setName(c.getString(c.getColumnIndex("name")));
		t.setDescription(c.getString(c.getColumnIndex("description")));
		t.setPriority(Priority.values()[c.getInt(c.getColumnIndex("priority"))]);
		t.setStatus(Status.values()[c.getInt(c.getColumnIndex("status"))]);
		t.setModified(c.getLong(c.getColumnIndex("modified")));
		String creationDateString = c.getString(c.getColumnIndex("creationdate"));
		if (creationDateString != null)
			t.setCreationDate(new Date(creationDateString));
		String dueDateString = c.getString(c.getColumnIndex("duedate"));
		if (dueDateString != null)
			t.setDueDate(new Date(dueDateString));
		String completedDateString = c.getString(c.getColumnIndex("completeddate"));
		if (completedDateString != null)
			t.setCompletedDate(new Date(completedDateString));
		// XXX Project project;	// what this task is part of
		// XXX Context context;	// where to do it
		return t;
	}
	
	static void dumpCursor(Cursor c) {
		int n = c.getColumnCount();
		for (int i = 0; i < n; i++) {
			System.out.print(c.getColumnName(i));
			System.out.print("\t");
		}
		System.out.println();
		for (int i = 0; i < n; i++) {
			// bleah
			System.out.println(c.getInt(i));
		}
	}
	
	public static List<AndroidTask> cursorToTaskList(Cursor c) {
		List<AndroidTask> list = new ArrayList<>();
		while (c.moveToNext()) {
			list.add(cursorToTask(c));
		}
		return list;
	}

	/**
	 * Convert JSON goo like this:
	 * [{"id":102,"priority":"High","name":"TEST 2",
	 * "creationDate":{"year":2015,"month":11,"day":1},
	 * "project":null,"context":{"id":100,"name":"Life"},
	 * "dueDate":null,"status":"ACTIVE","completedDate":null,
	 * "modified":1448292432791,"description":"None","complete":false},
	 * {"id":103,"priority":"Low","name":"Low prio item",
	 * "creationDate":{"year":2015,"month":10,"day":23},"project":null,
	 * "context":{"id":80,"name":"Home"},"dueDate":null,"status":"NEW",
	 * "completedDate":null,"modified":1448312330383,
	 * "description":"","complete":false}]
	 * into a proper List<Task>.
	 */
	public static List<AndroidTask> jsonStringToListTask(String resultStr) {
		List<AndroidTask> ret = new ArrayList<>();
		try {
			JSONArray array = (JSONArray) new JSONTokener(resultStr).nextValue();
			for (int i = 0; i < array.length(); i++) {
				JSONObject o = array.getJSONObject(i);
				AndroidTask t = new AndroidTask();
				t.setId(o.getLong("id"));
				t.setPriority(Priority.valueOf(o.getString("priority")));
				t.setStatus(Status.valueOf(o.getString("status")));
				t.setName(o.getString("name"));
				t.setDescription(o.getString("description"));
				String creationDateString = o.getString("creationDate");
				if (creationDateString != null && !"null".equals(creationDateString))
					t.setCreationDate(new Date(creationDateString));
				t.setModified(o.getLong("modified"));
				String dueDateString = o.getString("dueDate");
				if (dueDateString != null && !"null".equals(dueDateString))
					t.setDueDate(new Date(dueDateString));
				String completedDateString = o.getString("completedDate");
				if (completedDateString != null && !"null".equals(completedDateString))
					t.setCompletedDate(new Date(completedDateString));
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


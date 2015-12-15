package todomore.android;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.darwinsys.todo.model.Priority;
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

	public static ContentValues taskToContentValues(Task t) {
		ContentValues cv = new ContentValues();
		cv.put("id", t.getId());
		if (t instanceof AndroidTask) {
			cv.put("_id", ((AndroidTask)t)._id);
		}
		cv.put("name", t.getName());
		cv.put("priority", t.getPriority().ordinal());
//		Date creationDate = new Date(); // when you decided you had to do it
//		Project project;	// what this task is part of
//		Context context;	// where to do it
//		Date dueDate;		// when to do it by
//		Status status;
//		Date completedDate = null; // when you actually did it
		cv.put("modified", t.getModified());
//		String description;
		return cv;
	}

	public static Task cursorToTask(Cursor c) {
		if (c.isAfterLast()) {
			Log.d(TAG, "Cursor has no more rows");
			return null;
		}
		AndroidTask t = new AndroidTask();
		t._id = c.getInt(1);
		t.setName(c.getString(c.getColumnIndex("name")));
		t.setPriority(Priority.values()[(c.getColumnIndex("priority"))]);
		t.setModified(c.getLong(c.getColumnIndex("modified")));
		return t;
	}
	
	public static List<Task> cursorToTaskList(Cursor c) {
		List<Task> list = new ArrayList<>();
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
	public static List<Task> jsonStringToListTask(String resultStr) {
		List<Task> ret = new ArrayList<>();
		try {
			JSONArray array = (JSONArray) new JSONTokener(resultStr).nextValue();
			for (int i = 0; i < array.length(); i++) {
				JSONObject o = array.getJSONObject(i);
				Task t = new Task();
				t.setId(o.getLong("id"));
				t.setPriority(Priority.valueOf(o.getString("priority")));
				t.setName(o.getString("name"));
				t.setModified(o.getLong("modified"));
				// XXX moar!!
				ret.add(t);
			}
		} catch (JSONException e) {
			throw new RuntimeException("This rudpucker failed to parse! " + e, e);
		}
		return ret;
	}
}


package todomore.android;

import java.util.ArrayList;
import java.util.List;

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
	
	public static List<Task> cursorToTasks(Cursor c) {
		List<Task> list = new ArrayList<>();
		while (c.moveToNext()) {
			list.add(cursorToTask(c));
		}
		return list;
		}
}


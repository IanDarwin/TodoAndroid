package todomore.android;

import com.darwinsys.todo.model.Context;
import com.darwinsys.todo.model.Date;
import com.darwinsys.todo.model.Priority;
import com.darwinsys.todo.model.Project;
import com.darwinsys.todo.model.Status;
import com.darwinsys.todo.model.Task;

import android.content.ContentValues;

/** 
 * This class encapsulates various make-work functions that 
 * WOULD NOT BE NEEDED IF ANDROID'S API WERE OBJECT ORIENTED!
 * @author Ian Darwin
 */
public class GruntWork {

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
}

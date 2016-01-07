package todomore.android;

import android.app.Application;
import todomore.android.TaskDao;

public class TodoMoreApplication extends Application {

	private TaskDao mTaskDao;
	
	@Override
	public void onCreate() {
		super.onCreate();
		mTaskDao = new TaskDao(this);
	}

	public TaskDao getTaskDao() {
		return mTaskDao;
	}
}

package todomore.android;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;

public class TodoMoreApplication extends Application {

	private TaskDao mTaskDao;
	private static SharedPreferences mPrefs;
	
	@Override
	public void onCreate() {
		super.onCreate();
		mTaskDao = new TaskDao(this);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);;
	}

	public TaskDao getTaskDao() {
		return mTaskDao;
	}
	
	public static String makeBasicAuthString() {
		String userName = mPrefs.getString(MainActivity.KEY_USERNAME, "nonesuch");
        String password = mPrefs.getString(MainActivity.KEY_PASSWORD, "don't use this");
        String authInfo = "Basic " + Base64.encodeToString(
            (userName + ":" + password).getBytes(), 0);
        return authInfo;
	}
}

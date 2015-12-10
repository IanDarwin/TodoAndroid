package todomore.android;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AppSingleton extends Application {

    private static AppSingleton sInstance;

	private SharedPreferences mPrefs;
	private String mUser, mPassword;

    public static AppSingleton getInstance() {
      return sInstance;
    }

    @Override
    public void onCreate() {
      super.onCreate();  
      sInstance = this;
      sInstance.initializeInstance();
    }

    private void initializeInstance() {
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }
    
    public SharedPreferences getPrefs() {
    	return mPrefs;
    }

	public String getUserName() {
		return mUser;
	}
	public void setUserName(String username) {
		this.mUser = username;
	}

	public String getPassword() {
		return mPassword;
	}
	public void setPassword(String password) {
		this.mPassword = password;
	}
}

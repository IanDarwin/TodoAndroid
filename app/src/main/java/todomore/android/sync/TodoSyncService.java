package todomore.android.sync;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

/** 
 * This will be the Synchronizer service, for use after the user has signed in successfully.
 * Pretty basic SyncService, based on documentation and examples.
 * @author Ian Darwin
 */
public class TodoSyncService extends Service {

	private String TAG = "TodoSyncService";
	private TodoSyncAdapter sSyncAdapter;
	private static final Object sLock = new Object();

	@Override
	public void onCreate() {
		Log.d(TAG, "TodoSyncService.onCreate()");
		super.onCreate();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
		synchronized(sLock) {
			if (sSyncAdapter == null) {
				sSyncAdapter = new TodoSyncAdapter(getApplicationContext(), prefs, true);
			}
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return sSyncAdapter.getSyncAdapterBinder();
	}
}

package todomore.android;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/** 
 * This will be the Synchronizer service, for use after the user has signed in successfully.
 * Pretty basic SyncService, based on documentation and examples.
 * @author Ian Darwin
 */
public class TodoSyncService extends Service {

	private TodoSyncAdapter sSyncAdapter;
	private static final Object sLock = new Object();

	@Override
	public void onCreate() {
		super.onCreate();
		synchronized(sLock) {
			if (sSyncAdapter == null) {
				sSyncAdapter = new TodoSyncAdapter(getApplicationContext(), true);
			}
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return sSyncAdapter.getSyncAdapterBinder();
	}
}

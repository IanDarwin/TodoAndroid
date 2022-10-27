package todomore.android.sync;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Service for the useless dummy Authenticator.
 */
public class TodoDummyAuthenticatorService extends Service {
	
		// Instance field that stores the authenticator object,
		// so we only create it once for multiple uses.
		private TodoDummyAuthenticator mAuthenticator;
		
		@Override
		public void onCreate() {
			// Create the authenticator object
			mAuthenticator = new TodoDummyAuthenticator(this);
		}
		/*
		 * Called when the system binds to this Service to make the IPC call;
		 * just return the authenticator's IBinder.
		 */
		@Override
		public IBinder onBind(Intent intent) {
			return mAuthenticator.getIBinder();
		}
	}

package todomore.android;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class NetworkService extends IntentService {

	private static final String TAG = NetworkService.class.getSimpleName();
	
	public NetworkService() {
		super("TodoMore:Android-Network-Service");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "NetworkService.onHandleIntent()");
	}

}

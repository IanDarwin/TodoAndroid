package todomore.android;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import todomore.android.netio.UrlConnector;
import todomore.android.sync.TodoSyncAdapter;


/**
 * A preferences screen that offers username/password and other settings
 */
public class PrefsActivity extends Activity {

	public static final String TAG = PrefsActivity.class.getName();
	
    static final int DEFAULT_MINUTES_INTERVAL = 60;

	private SharedPreferences mPrefs;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set up defaults before creating PrefsFragment
        PreferenceManager.setDefaultValues(this, R.layout.prefs, false);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        getFragmentManager().beginTransaction()
        	.replace(android.R.id.content, new PrefsFragment())
        	.commit();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.prefs_menu, menu);
		return true;
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.test_menuitem:
			testNetworkSetup();
			return true;
		case R.id.done_menuitem:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public static class PrefsFragment extends PreferenceFragment {
    	public void onCreate(Bundle savedInstanceState) {
    		super.onCreate(savedInstanceState);
    		addPreferencesFromResource(R.layout.prefs);
    	}
    }
	
    private void testNetworkSetup() {
		AsyncTask<Void, String, Boolean> execute = new AsyncTask<Void, String, Boolean>() {

			@Override
			public void onProgressUpdate(String... values) {
				Toast.makeText(PrefsActivity.this, values[0], Toast.LENGTH_LONG).show();
			}

			@Override
			protected Boolean doInBackground(Void... args) {
				try {
					URL url = TodoSyncAdapter.makeRequestUrl(mPrefs, "/status");
					Map<String, String> headers = new HashMap<>();
					headers.put("Accept", "application/json");
					headers.put("Authorization", TodoMoreApplication.makeBasicAuthString());
					String response = UrlConnector.converse(url, null, headers);
					publishProgress("Connection OK ");
					JSONObject result = new JSONObject(response);
					String status = "OK! " + result.getString("status");
					publishProgress(status);
					return Boolean.TRUE;
				} catch (Exception e) {
					final String message = "REST error: " + e;
					Log.e(TAG, message, e);
					publishProgress(message);
					return Boolean.FALSE;
				}
			}
		}.execute();
	}
}



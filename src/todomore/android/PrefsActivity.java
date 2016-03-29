package todomore.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
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
		new AsyncTask<Void, String, Boolean>() {
		
			@Override
			public void onProgressUpdate(String... values) {
				Toast.makeText(PrefsActivity.this, values[0], Toast.LENGTH_LONG).show();
			};
			
			@Override
			protected Boolean doInBackground(Void... args) {
				try {
					URL url = TodoSyncAdapter.makeRequestUrl(mPrefs, "/status");
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.addRequestProperty("Authorization", makeBasicAuthString(mPrefs));
					InputStream is = conn.getInputStream();
					publishProgress("Connection status code " + conn.getResponseCode());
					String response = read(is);
					JSONObject result = new JSONObject(response);
					String status = "OK! " + result.getString("status");
					publishProgress(status);
					return Boolean.TRUE;
				} catch (IOException | JSONException e) {
					final String message = "REST error: " + e;
					Log.e(TAG, message, e);
					publishProgress(message);
					return Boolean.FALSE;
				}
			}
		}.execute();
	}

	private String makeBasicAuthString(SharedPreferences mPrefs) {
		String userName = mPrefs.getString(MainActivity.KEY_USERNAME, "nonesuch");
        String password = mPrefs.getString(MainActivity.KEY_PASSWORD, "don't use this");
        String authInfo = "Basic " + Base64.encodeToString(
            (userName + ":" + password).getBytes(), 0);
        return authInfo;
	}
	
    private static String read(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(in), 1000);
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            sb.append(line);
        }
        in.close();
        return sb.toString();
    }
}



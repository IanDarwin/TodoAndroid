package todomore.android;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.prefs.Preferences;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.darwinsys.todo.model.Priority;
import com.darwinsys.todo.model.Task;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

public class MainActivity extends Activity {

	public static final String TAG = MainActivity.class.getSimpleName();
	private EditText addTF;
	private Spinner prioSpinner;
	private ListView mListView;
	private TaskDao mDao;
	private int ACTIVITY_ID_LOGIN;
	private SharedPreferences mPrefs;
	// Keys for mPrefs lookups
	private String KEY_USERNAME, KEY_PASSWORD, KEY_HOSTNAME, KEY_HOSTPORT, KEY_HOSTPATH;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        addTF = (EditText) findViewById(R.id.addTF);
        prioSpinner = (Spinner) findViewById(R.id.prioSpinner);
        mDao = new TaskDao(this);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.priorities_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        KEY_USERNAME = getString(R.string.key_username);
        KEY_PASSWORD = getString(R.string.key_password);
        KEY_HOSTNAME = getString(R.string.key_hostname);
        KEY_HOSTPORT = getString(R.string.key_hostport);
        KEY_HOSTPATH = getString(R.string.key_hostpath);

        prioSpinner.setAdapter(adapter);
        
        ensureLogin();
    }
    
    @Override
    protected void onRestart() {
    	super.onRestart();
    	addTF.requestFocus();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.main_menu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.settings_menuitem:
    		startActivity(new Intent(this, PrefsActivity.class));
    		return true;
    	case R.id.help_menuitem:
    		Toast.makeText(this, "Help not written yet", Toast.LENGTH_SHORT).show();
    		return true;
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }
    
    /** Called from the View when the Add button is pressed */
    public void addItem(View v) {
    	String name = addTF.getText().toString();
    	Log.d(TAG, "addItem: " + name);
    	if (name == null || name.isEmpty()) {
    		Toast.makeText(this, "Text required!", Toast.LENGTH_SHORT).show();
    		return;
    	}
    	ensureLogin();
    	
    	// Do the work here! Save to local DB before sending to the server...
    	AndroidTask t = new AndroidTask();
    	t.setName(addTF.getText().toString());
    	t.setPriority(Priority.values()[prioSpinner.getSelectedItemPosition()]);
    	t.setModified(System.currentTimeMillis());
    	
    	long _id = mDao.insert(t);
    	t._id = _id;
    	Toast.makeText(this, "Saved locally", Toast.LENGTH_SHORT).show();
    	
    	// XXX Send to server now; later, will just trigger sync?
    	new SendObjectAsyncTask().execute(t);
    	
    	// If we get here, remove text from TF so task doesn't get added twice
    	addTF.setText("");
    	
    	Toast.makeText(this, "Sync request send", Toast.LENGTH_SHORT).show();
    	new GetListAsyncTask().execute();
    }
    
    private boolean isLoginOK;
    
    /** 
     * If the user hasn't provided username and password, press them for credentials
     */
    private void ensureLogin() {
    	if (!isLoginOK) {
    		String userName = mPrefs.getString(KEY_USERNAME, null);
    		String password = mPrefs.getString(KEY_PASSWORD, null);
    		if (userName == null || userName.isEmpty() ||
    				password == null || password.isEmpty()) {
    			startActivityForResult(new Intent(this, PrefsActivity.class), ACTIVITY_ID_LOGIN);
    		}
    	}
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode != ACTIVITY_ID_LOGIN) {
			super.onActivityResult(requestCode, resultCode, data);
			return;
		}
		// Not secure enough for general use, but since the eserver side will actually
		// validate the credentials, this is OK for use in this app.
		if (resultCode == RESULT_OK) {
			isLoginOK = true;
		}
	}
	
	private String getUserName() { return mPrefs.getString(KEY_USERNAME, null); }
	private String getPassword() { return mPrefs.getString(KEY_PASSWORD, null); }
	
	/** Should be used by all REST tasks */
	protected URI makeRestUri(SharedPreferences prefs) {
		try {
			return new URI(String.format("http://%s:%d/%s/todo/%s/tasks", 
					mPrefs.getString(KEY_HOSTNAME, null),
					Integer.parseInt(mPrefs.getString(KEY_HOSTPORT, "80")),
					mPrefs.getString(KEY_HOSTPATH, "/"), getUserName()));
		} catch (URISyntaxException e) {
			throw new RuntimeException("Failed to create path! " + e, e);
		}
	}
    
	public class SendObjectAsyncTask extends AsyncTask<Task, Void, Long>{
		final ObjectMapper jacksonMapper = new ObjectMapper();

		@Override
		protected Long doInBackground(Task... params) {
			ensureLogin();
    		
			// The number shalle be one...
			Task t = params[0];
			Log.d(TAG, "Starting TODO send of task " + t + " for user " + getUserName());
			
			HttpClient client = new DefaultHttpClient();
			Credentials creds = new UsernamePasswordCredentials(getUserName(), getPassword());        
			((AbstractHttpClient)client).getCredentialsProvider()
				.setCredentials(new AuthScope(
						mPrefs.getString(KEY_HOSTNAME, "10.0.2.2"), 
						Integer.parseInt(mPrefs.getString(KEY_HOSTPORT, "80"))),
						creds); 
			try {
				final URI postUri = makeRestUri(mPrefs);
			
				// Send a POST request with to upload this Task
				Log.d(TAG, "Connecting to server for " + postUri);

				HttpPost postAccessor = new HttpPost();
				postAccessor.setURI(postUri);
				postAccessor.addHeader("Content-Type", "application/json");
				postAccessor.addHeader("Accept", "application/json");
				
				final ObjectWriter w = jacksonMapper.writer();
				String json = w
				  .with(SerializationFeature.INDENT_OUTPUT)
				  .without(SerializationFeature.WRAP_EXCEPTIONS)
				  .writeValueAsString(t);

				postAccessor.setEntity(new StringEntity(json));

				// INVOKE
				HttpResponse response = client.execute(postAccessor);

				// Get the response body from the response
				HttpEntity postResults = response.getEntity();
				final String resultStr = EntityUtils.toString(postResults);
				Log.d(TAG, "Result from SEND: " + resultStr);
				
				// it actually sends the URL of the new ID
				Uri resultUri = Uri.parse(resultStr);
				long id = ContentUris.parseId(resultUri);
				t.setId(id);
				if (!mDao.update(t)) {
					Log.e(TAG, "FAILED TO UPDATE");
				}
				Log.d(TAG, "UPDATED " + t + ", new _ID = " + t.getId());
				return id;
			} catch (RuntimeException e) { // Avoid bloating the stack trace
				throw e;
			} catch (Exception e) {
				throw new RuntimeException("Send failed!" + e, e);
			}
		}

	}
	
	public class GetListAsyncTask extends AsyncTask<Void, Void, List<Task>>{
		final ObjectMapper jacksonMapper = new ObjectMapper();

		@Override
		protected List<Task> doInBackground(Void... params) {
			Log.d(TAG, "Starting TODO list-fetch for " + getUserName());
			
			HttpClient client = new DefaultHttpClient();
			Credentials creds = new UsernamePasswordCredentials(getUserName(), getPassword());        
			((AbstractHttpClient)client).getCredentialsProvider()
			.setCredentials(new AuthScope(
					mPrefs.getString(KEY_HOSTNAME, "10.0.2.2"), 
					Integer.parseInt(mPrefs.getString(KEY_HOSTPORT, "80"))),
					creds);  
			try {
				final URI postUri = makeRestUri(mPrefs);
			
				// Send a GET request with to list the Tasks
				Log.d(TAG, "Connecting to server for " + postUri);

				HttpGet httpAccessor = new HttpGet();
				httpAccessor.setURI(postUri);
				httpAccessor.addHeader("Accept", "application/json");
				
				// INVOKE
				HttpResponse response = client.execute(httpAccessor);

				// Get the response body from the response
				HttpEntity postResults = response.getEntity();
				final String resultStr = EntityUtils.toString(postResults);
				Log.d(TAG, "Result from LIST: " + resultStr);

				// Service sends the list of Tasks in JSON
				List<Task> list = GruntWork.jsonStringToListTask(resultStr);
				return list;
			} catch (Exception e) {
				throw new RuntimeException("Get List failed!" + e, e);
			}
		}
		
		@Override
		protected void onPostExecute(List<Task> result) {
			ListAdapter adapter = new ArrayAdapter<Task>(MainActivity.this, 0);
			mListView.setAdapter(adapter);
		}
	}
}

package todomore.android;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
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
import com.darwinsys.todo.model.Status;
import com.darwinsys.todo.model.Task;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import todomore.android.TodoMoreApplication;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

public class MainActivity extends Activity {

	public static final String TAG = MainActivity.class.getName();
	private EditText addTF;
	private Spinner prioSpinner;
	private ListView mListView;
	private int ACTIVITY_ID_LOGIN;
	private static SharedPreferences mPrefs;
	// Keys for mPrefs lookups
	public static String KEY_USERNAME;
	public static String KEY_PASSWORD;
	public static String KEY_HOSTNAME;
	public static String KEY_HOSTPORT;
	public static String KEY_HOSTPATH;
	public static String KEY_ENABLE_SYNCH ;

	/** The account name */
    public static final String ACCOUNT = "account";
    /* The account */
	private static Account mAccount;
	
	// Data
	List<String> fullTitlesList;
	List<AndroidTask> fullTaskList;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		addTF = (EditText) findViewById(R.id.addTF);
		prioSpinner = (Spinner) findViewById(R.id.prioSpinner);
		mListView = (ListView) findViewById(R.id.listView);
		mListView.setOnItemClickListener(new OnItemClickListener() {
			//@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				AndroidTask androidTask = (AndroidTask)fullTaskList.get(position);
				long _id = androidTask.get_Id();
				Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
				intent.putExtra("taskId", _id);
				startActivity(intent);
			}
		});

		ArrayAdapter<CharSequence> adapter = 
			ArrayAdapter.createFromResource(this, R.array.priorities_array,
				android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		prioSpinner.setAdapter(adapter);
		prioSpinner.setSelection(Priority.High.ordinal());

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		KEY_ENABLE_SYNCH = getString(R.string.key_enable_synch);
		KEY_USERNAME = getString(R.string.key_username);
		KEY_PASSWORD = getString(R.string.key_password);
		KEY_HOSTNAME = getString(R.string.key_hostname);
		KEY_HOSTPORT = getString(R.string.key_hostport);
		KEY_HOSTPATH = getString(R.string.key_hostpath);

		mAccount = createSyncAccount(this);
		enableSynching(mPrefs.getBoolean(KEY_ENABLE_SYNCH, false));
		
		mPrefs.registerOnSharedPreferenceChangeListener(new MyPrefsListener());
		loadListFromDB();
	}
	
	private class MyPrefsListener implements SharedPreferences.OnSharedPreferenceChangeListener {
		public void onSharedPreferenceChanged (SharedPreferences sharedPreferences, String key) {
			if (MainActivity.KEY_ENABLE_SYNCH.equals(key)) {
				final boolean enable = sharedPreferences.getBoolean(MainActivity.KEY_ENABLE_SYNCH, false);
				Log.d(TAG, "MainActivity.MyPrefsListener.onSharedPreferenceChanged(SYNCH, " + enable + ")");
				enableSynching(enable);
			}
		}
	}

	void enableSynching(boolean enable) {
		Log.d(TAG, "MainActivity.enableSynching(): " + enable);
		String authority = getString(R.string.datasync_provider_authority);
		Bundle extras = new Bundle();
		if (enable) {
			ContentResolver.setSyncAutomatically(mAccount, authority, true);
			// Force immediate - will probably remove this later
			Bundle immedExtras = new Bundle();
			immedExtras.putBoolean("SYNC_EXTRAS_MANUAL", true);
			ContentResolver.requestSync(mAccount, authority, immedExtras);

			// Request hourly synching - TODO add a prefs for the interval.
			long pollFrequency = PrefsActivity.DEFAULT_MINUTES_INTERVAL * 60;
			ContentResolver.addPeriodicSync(mAccount, authority, extras, pollFrequency);
		} else {
			// Cancel all outstanding syncs until further notice
			ContentResolver.cancelSync(mAccount, authority);
		}
	}
	/**
     * Create a new dummy account for the sync adapter.
     * @author Adapted from http://developer.android.com/ page on this topic.
     * @param context The application context
     */
    public Account createSyncAccount(Context context) {
        // Create the account type and default account
        Account newAccount = new Account(ACCOUNT, getString(R.string.accountType));
        // Get the Android account manager
        AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
        Log.d(TAG, "Adding Account Explicitly");
        if (accountManager.addAccountExplicitly(newAccount, "top secret", null)) {
            Log.d(TAG, "Added Account Explicitly Successfully");
        } else {
        	Log.d(TAG, "Account exists, or, other error");
        }
        return newAccount;
    }

	@Override
	protected void onResume() {
		super.onResume();
		addTF.requestFocus();
		loadListFromDB();
	}
	
	private void loadListFromDB() {
		fullTaskList = ((TodoMoreApplication)getApplication()).getTaskDao().findAll();
		fullTitlesList = new ArrayList<String>();
		for (Task t : fullTaskList) {
			fullTitlesList.add(t.getName());
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
				MainActivity.this, android.R.layout.simple_list_item_1, fullTitlesList);
		mListView.setAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings_menuitem:
			startActivity(new Intent(this, PrefsActivity.class));
			return true;
		case R.id.help_menuitem:
			startActivity(new Intent(this, HelpActivity.class));
			return true;
		case R.id.about_menuitem:
			startActivity(new Intent(this, AboutActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/** 
	 * Adds a new item to the list, from the main screen.
	 * Called from the View when the Add button is pressed;
	 * registered via onClick= so no Listener code
	 */
	public void addItem(View v) {
		String name = addTF.getText().toString();
		Log.d(TAG, "addItem: " + name);
		if (name == null || name.isEmpty()) {
			Toast.makeText(this, "Text required!", Toast.LENGTH_SHORT).show();
			return;
		}

		// Do the work here! Save to local DB, let Sync Adapter send it to the server...
		AndroidTask t = new AndroidTask();
		t.setName(addTF.getText().toString());
		t.setPriority(Priority.values()[prioSpinner.getSelectedItemPosition()]);
		t.setModified(System.currentTimeMillis());
		t.setStatus(Status.NEW);

		long _id = ((TodoMoreApplication) getApplication()).getTaskDao().insert(t);
		Toast.makeText(this, "Saved locally, _id = " + _id, Toast.LENGTH_SHORT).show();

		// Will schedule later; for now just trigger sync
		Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        
        /*
         * Request the sync for the default account, authority, and
         * manual sync settings
         */
        ContentResolver.requestSync(mAccount, getString(R.string.datasync_provider_authority), settingsBundle);

		// If we get here, remove text from TF so task doesn't get added twice
		addTF.setText("");

		// And update the list
		loadListFromDB();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode != ACTIVITY_ID_LOGIN) {
			super.onActivityResult(requestCode, resultCode, data);
			return;
		}
	}

	protected static URI makeSendUri(SharedPreferences prefs) {
		try {
			String pathStr = prefs.getString(KEY_HOSTPATH, "/");
			return new URI(String.format("http://%s:%d/%s/new/tasks", 
					prefs.getString(KEY_HOSTNAME, null),
					Integer.parseInt(prefs.getString(KEY_HOSTPORT, "80")),
					pathStr.startsWith("/") ? pathStr.substring(1) : pathStr));
		} catch (URISyntaxException e) {
			throw new RuntimeException("Failed to create path! " + e, e);
		}
	}
	protected static URI makeListUri(SharedPreferences prefs) {
		try {
			String pathStr = prefs.getString(KEY_HOSTPATH, "/");
			return new URI(String.format("http://%s:%d/%s/%s/tasks", prefs.getString(KEY_HOSTNAME, null),
					Integer.parseInt(prefs.getString(KEY_HOSTPORT, "80")),
					pathStr.startsWith("/") ? pathStr.substring(1) : pathStr, mPrefs.getString(KEY_USERNAME, null)));
		} catch (URISyntaxException e) {
			throw new RuntimeException("Failed to create path! " + e, e);
		}
	}

	// This code must die...
	private class SendObjectAsyncTask extends AsyncTask<AndroidTask, Void, Long> {
		final ObjectMapper jacksonMapper = new ObjectMapper();

		@Override
		protected Long doInBackground(AndroidTask... params) {
			// ensureLogin();

			// The number shalle be one...
			AndroidTask t = params[0];
			Log.d(TAG, "Starting TODO send of task " + t + " for user " + mPrefs.getString(KEY_USERNAME, null));

			HttpClient client = new DefaultHttpClient();
			Credentials creds = new UsernamePasswordCredentials(
					mPrefs.getString(KEY_USERNAME, null), 
					mPrefs.getString(KEY_PASSWORD, null));
			((AbstractHttpClient) client).getCredentialsProvider()
					.setCredentials(new AuthScope(mPrefs.getString(KEY_HOSTNAME, "10.0.2.2"),
							Integer.parseInt(mPrefs.getString(KEY_HOSTPORT, "80"))), creds);
			try {
				final URI postUri = makeSendUri(mPrefs);

				// Send a POST request with to upload this Task
				Log.d(TAG, "Connecting to server for " + postUri);

				HttpPost postAccessor = new HttpPost();
				postAccessor.setURI(postUri);
				postAccessor.addHeader("Content-Type", "application/json");
				postAccessor.addHeader("Accept", "text/plain");

				final ObjectWriter w = jacksonMapper.writer();
				String json = w.with(SerializationFeature.INDENT_OUTPUT).without(SerializationFeature.WRAP_EXCEPTIONS)
						.writeValueAsString(t);

				postAccessor.setEntity(new StringEntity(json));

				// INVOKE
				HttpResponse response = client.execute(postAccessor);

				// Get the response body from the response
				StatusLine stat = response.getStatusLine();
				final int resp = stat.getStatusCode();
				Log.d(TAG, "Result from SEND: " + resp);

				// on success it should send us the URL of the new ID, we need to save the remote id in our db
				if (resp != 201) {
					runOnUiThread(new Runnable() { 
						public void run() {
							Toast.makeText(MainActivity.this, "Failed to create " + resp, Toast.LENGTH_LONG).show();
						}
					});
					return -1L;
				}
				String resultStr = response.getFirstHeader("location").getValue();
				Uri resultUri = Uri.parse(resultStr);
				long id = ContentUris.parseId(resultUri);
				t.setId(id);
				if (!((TodoMoreApplication) getApplication()).getTaskDao().update(t)) {
					Log.e(TAG, "FAILED TO UPDATE");
				}
				Log.d(TAG, "UPDATED " + t + ", new Remote ID = " + t.getId());
				return id;
			} catch (RuntimeException e) { // Avoid bloating the stack trace
				throw e;
			} catch (Exception e) {
				throw new RuntimeException("Send failed!" + e, e);
			}
		}
	}

	private class GetListAsyncTask extends AsyncTask<Void, Void, List<AndroidTask>> {
		
		@Override
		protected List<AndroidTask> doInBackground(Void... params) {
			Log.d(TAG, "Starting TODO list-fetch for " + mPrefs.getString(KEY_USERNAME, null));

			HttpClient client = new DefaultHttpClient();
			Credentials creds = new UsernamePasswordCredentials(mPrefs.getString(KEY_USERNAME, null), mPrefs.getString(KEY_PASSWORD, null));
			((AbstractHttpClient) client).getCredentialsProvider()
					.setCredentials(new AuthScope(mPrefs.getString(KEY_HOSTNAME, "10.0.2.2"),
							Integer.parseInt(mPrefs.getString(KEY_HOSTPORT, "80"))), creds);
			try {
				final URI postUri = makeListUri(mPrefs);

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

				// Service sends the list of Tasks in JSON
				fullTaskList = GruntWork.jsonStringToListTask(resultStr);
				Log.d(TAG, "LIST SIZE = " + fullTaskList.size());
				// List is loaded into UI in onPostExecute() below...
				return fullTaskList;
			} catch (RuntimeException e) { // Avoid bloating the stack trace
				throw e;
			} catch (Exception e) {
				throw new RuntimeException("Get List failed!" + e, e);
			}
		}

		@Override
		protected void onPostExecute(List<AndroidTask> list) {
			fullTitlesList = new ArrayList<String>();
			for (Task t : list) {
				fullTitlesList.add(t.getName());
			}
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(
	                MainActivity.this, android.R.layout.simple_list_item_1, fullTitlesList);
	        mListView.setAdapter(adapter);
		}
	}
}

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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

public class MainActivity extends Activity {

	public static final String TAG = MainActivity.class.getSimpleName();
	private EditText addTF;
	private Spinner prioSpinner;
	private ListView mListView;
	private int ACTIVITY_ID_LOGIN;
	private static SharedPreferences mPrefs;
	// Keys for mPrefs lookups
	private static String KEY_USERNAME, KEY_PASSWORD, KEY_HOSTNAME, KEY_HOSTPORT, KEY_HOSTPATH;
	
	// Data
	List<String> fullTitlesList;
	List<Task> fullTaskList;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		addTF = (EditText) findViewById(R.id.addTF);
		prioSpinner = (Spinner) findViewById(R.id.prioSpinner);
		mListView = (ListView) findViewById(R.id.listView);
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// This will de-convolute when we put in a real Adapter for the List.
				AndroidTask androidTask = (AndroidTask)fullTaskList.get(position);
				long _id = androidTask.get_Id();
				Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
				intent.putExtra("taskId", _id);
				startActivity(intent);
			}
		});

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.priorities_array,
				android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		prioSpinner.setAdapter(adapter);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		KEY_USERNAME = getString(R.string.key_username);
		KEY_PASSWORD = getString(R.string.key_password);
		KEY_HOSTNAME = getString(R.string.key_hostname);
		KEY_HOSTPORT = getString(R.string.key_hostport);
		KEY_HOSTPATH = getString(R.string.key_hostpath);

		ensureLogin();
	}

	@Override
	protected void onResume() {
		super.onResume();
		addTF.requestFocus();
		if (isLoginSet()) {
			new GetListAsyncTask().execute();
		}
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
		t.setStatus(Status.NEW);

		long _id = ((TodoMoreApplication) getApplication()).getTaskDao().insert(t);
		t.set_Id(_id);
		Toast.makeText(this, "Saved locally", Toast.LENGTH_SHORT).show();

		// XXX Send to server now; later, will just trigger sync?
		new SendObjectAsyncTask().execute(t);

		// If we get here, remove text from TF so task doesn't get added twice
		addTF.setText("");

		new GetListAsyncTask().execute();
	}

	private boolean isLoginSet() {
		String userName = mPrefs.getString(KEY_USERNAME, null);
		String password = mPrefs.getString(KEY_PASSWORD, null);
		return userName != null && !userName.isEmpty() && 
				password != null && !userName.isEmpty();
	}

	/**
	 * If the user hasn't provided username and password, press them for
	 * credentials
	 */
	private void ensureLogin() {
		if (!isLoginSet()) {
			String userName = mPrefs.getString(KEY_USERNAME, null);
			String password = mPrefs.getString(KEY_PASSWORD, null);
			if (userName == null || userName.isEmpty() || password == null || password.isEmpty()) {
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
	}

	private static String getUserName() {
		return mPrefs.getString(KEY_USERNAME, null);
	}

	private static String getPassword() {
		return mPrefs.getString(KEY_PASSWORD, null);
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
					pathStr.startsWith("/") ? pathStr.substring(1) : pathStr, getUserName()));
		} catch (URISyntaxException e) {
			throw new RuntimeException("Failed to create path! " + e, e);
		}
	}

	public class SendObjectAsyncTask extends AsyncTask<Task, Void, Long> {
		final ObjectMapper jacksonMapper = new ObjectMapper();

		@Override
		protected Long doInBackground(Task... params) {
			ensureLogin();

			// The number shalle be one...
			Task t = params[0];
			Log.d(TAG, "Starting TODO send of task " + t + " for user " + getUserName());

			HttpClient client = new DefaultHttpClient();
			Credentials creds = new UsernamePasswordCredentials(getUserName(), getPassword());
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

	public class GetListAsyncTask extends AsyncTask<Void, Void, List<Task>> {
		final ObjectMapper jacksonMapper = new ObjectMapper();

		@Override
		protected List<Task> doInBackground(Void... params) {
			Log.d(TAG, "Starting TODO list-fetch for " + getUserName());

			HttpClient client = new DefaultHttpClient();
			Credentials creds = new UsernamePasswordCredentials(getUserName(), getPassword());
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
		protected void onPostExecute(List<Task> list) {
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

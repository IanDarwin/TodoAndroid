package todomore.android.sync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import todomore.android.AndroidTask;
import todomore.android.MainActivity;
import todomore.android.R;
import todomore.android.TaskDao;
import todomore.android.TodoMoreApplication;
import todomore.android.netio.UrlConnector;

/**
 * Android Synch Adapter for Todo List Tasks;
 * write items to, and read items from, the REST server.
 * @author Ian Darwin
 */
public class TodoSyncAdapter extends AbstractThreadedSyncAdapter {

	private final static String TAG = TodoSyncAdapter.class.getName();

	private final static String LAST_SYNC_TSTAMP = "last sync";
	private long lastSynchTime = 0;

	private SharedPreferences mPrefs;
	private TaskDao mDao;

	private ObjectMapper jacksonMapper = new ObjectMapper();
	private DefaultHttpClient client; // XXX Only used in syncGetTasksFromRemote(client)
	private String pathStr;

	public TodoSyncAdapter(Context context, SharedPreferences prefs, boolean autoInitialize) {
		this(context, prefs, autoInitialize, false);
	}

	public TodoSyncAdapter(
			Context context,
			SharedPreferences prefs,
			boolean autoInitialize,
			boolean allowParallelSyncs) {
		super(context, autoInitialize, allowParallelSyncs);

		this.mPrefs = prefs;
		mDao = new TaskDao(context);
	}
	
	// Keys that must be set for synching to work.
	final static String[] keys = {
			MainActivity.KEY_HOSTNAME,
			MainActivity.KEY_HOSTPATH,
			MainActivity.KEY_USERNAME,
			MainActivity.KEY_PASSWORD
	};
	
	public static boolean isSynchEnabled(SharedPreferences mPrefs) {
		Log.d(TAG, "TodoSyncAdapter.synchIsEnabled()");
		if (!mPrefs.getBoolean(MainActivity.KEY_ENABLE_SYNCH, false)) {
			System.out.println("TodoSyncAdapter.isSynchEnabled(): FAIL ON B " + MainActivity.KEY_ENABLE_SYNCH);
			// return false;
		}
		for (String k : keys) {
			if (mPrefs.getString(k, null) == null) {
				System.out.println("TodoSyncAdapter.isSynchEnabled(): FAIL ON S " + k);
				// return false;
			};
		}
		return true;
	}
	
	public static boolean isHttps(SharedPreferences mPrefs) {
		Log.d(TAG, "TodoSyncAdapter.isHttps()");
		return mPrefs.getBoolean(MainActivity.KEY_HOST_HTTPS, true);
	}
	
    public static String read(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(in), 1000);
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            sb.append(line);
        }
        in.close();
        return sb.toString();
    }

	/**
	 * Do the actual synch. One possible algorithm would be:
	 *
	 * Form list of entries with remoteids in local db
	 *
	 * Download list of tasks from remote.
	 *
	 * For each task in local db
	 *   If its remoteId is null, add it to upload list.
	 *   If its mtime is > lastsynchrime, add it "
	 *
	 * For each task in downloaded list
	 *     If its remoteId not in list, add to local db
	 *     If its mtime > lastsynchtime, update it into local db
	 * For each task in local Delete quueu
	 * 		Send a Delete request.
	 * Update lastsynchtime.
	 */
	@Override
	public void onPerformSync(Account account, 
			Bundle extras, 
			String authority,
			ContentProviderClient provider, 
			SyncResult syncResult) {
		Log.d(TAG, "ToDoSyncAdapter.onPerformSync()");
		
		if (!isSynchEnabled(mPrefs)) {
			Log.d(TAG, "onPerformSync called but not enabled");
			return;
		}

		lastSynchTime = mPrefs.getLong(LAST_SYNC_TSTAMP, 0L);
		ArrayList<AndroidTask> toSaveRemotely = new ArrayList<AndroidTask>();
		ArrayList<AndroidTask> toSaveLocally = new ArrayList<AndroidTask>();

		// Get the username and password, which must be in mPrefs by now
		String userName = mPrefs.getString("KEY_USERNAME", null);
		String password = mPrefs.getString("KEY_PASSWORD",  null);
		Log.d(TAG, String.format("userName %s, Password xxxxxxxx", userName));
		if (userName == null || userName.isEmpty() ||
				password == null || password.isEmpty()) {
			Log.d(TAG, "Can't synch until you set username and password in Preferences");
			return;
		}

		Log.d(TAG, "Starting TODO Sync for " + userName);

		HttpClient client = syncSetupRestConnection(userName, password);

		try {
			// Zeroeth, delete any remote items we previously deleted locally.
			syncRunDeleteQueue();

			// First, get list of items FROM the local DB and the remote server
			final List<AndroidTask> local = mDao.findAll();
			final List<AndroidTask> remote = syncGetTasksFromRemote(client);

			// NOW RUN LOGIC TO FIGURE OUT WHAT TO PUT WHERE
			TodoSyncAdapter.algorithm(local, remote, 
					lastSynchTime,
					toSaveRemotely, toSaveLocally);
			
			synchSaveOrUpdateLocalTasks(toSaveLocally);

			synchSaveOrUpdateRemoteTasks(toSaveRemotely);

			// Finally, update our synch timestamp!
			mPrefs.edit().putLong(LAST_SYNC_TSTAMP, System.currentTimeMillis());

			syncResult.clear();		// success
		} catch (Exception e) {
			Log.wtf(TAG, "ERROR in synchronization!: " + e, e);
			e.printStackTrace();
			syncResult.databaseError = true;
		}
	}

	/**
	 * THE LOGIC OF SYNCHRONIZATION IS HERE:
	 * If the object exists here but not remotely, add to toSaveRemotely;
	 * If the object exists here and is modified more recently than lastSynchTime, ditto;
	 * Then the same for objects that exist remotely but not here, or, remotely and modified.
	 * WARNING: Does not handle the case of both versions being modified!
	 * Isolated to just deal with Lists, to make testing easier(possible).
	 * @param local The list of existing Tasks in the local database
	 * @param remote The list of existing Tasks in the remote database
	 * @param lastSyncTime  Completion time viewed locally when last synch operation finished.
	 * @param toSaveLocally  The list of Tasks to be saved (added OR updated) locally
	 * @param toSaveRemotely The list of Tasks to be saved (added OR updated) remotely
	 * @param toDeleteRemotely The list of Tasks to be deleted locally.
	 */
	public static void algorithm(List<AndroidTask> local, List<AndroidTask> remote, 
			long lastSynchTime, 
			List<AndroidTask> toSaveLocally, List<AndroidTask> toSaveRemotely) {

		// Pre-compute list of entries in local DB that have remote ids.
		List<Integer> localsWithRemoteId = new ArrayList<>();
		for (AndroidTask t : local) {
			if (t.getRemoteId() != 0) {
				localsWithRemote.add(t.getRemoteId());
			}
		}

		// Compute the list of remote tasks that must be saved/updated locally
		for (AndroidTask t : remote) {
			if (t.get_Id() == 0) {
				toSaveLocally.add(t);	// it's newly created remotely
				continue;
			}
			if (t.getModified() > lastSynchTime) {
				toSaveLocally.add(t);	// remote version was modified after last synch
				continue;
			}
		}

		// Compute the list of local tasks that must be sent to the server.
		for (AndroidTask t : local) {
			if (t.getRemoteId() == 0 || (t.getModified() > lastSynchTime)) {
				toSaveRemotely.add(t);
				continue;
			}
		}
	}

	// XXX Only used in syncGetTasksFromRemote(client)
	private HttpClient syncSetupRestConnection(String userName, String password) {
		client = new DefaultHttpClient();
		Credentials creds = new UsernamePasswordCredentials(userName, password);
		int port = Integer.parseInt(mPrefs.getString(MainActivity.KEY_HOSTPORT, "-1"));
		if ((port == -1 || port == 80) && isHttps(mPrefs)) {
			port = 443;
		} else {
			if (port == -1)
				port = 80;
		}
		((AbstractHttpClient)client).getCredentialsProvider()
		.setCredentials(
			new AuthScope(mPrefs.getString(MainActivity.KEY_HOSTNAME, "10.0.2.2"), port),
			creds);
		return client;
	}
	
	public static URL makeRequestUrl(SharedPreferences mPrefs, String finalPath) {
		StringBuilder sb = new StringBuilder();
		sb.append(isHttps(mPrefs) ? "https" : "http")
			.append("://")
			.append(mPrefs.getString(MainActivity.KEY_HOSTNAME, "localhost"));
		int port = Integer.parseInt(mPrefs.getString(MainActivity.KEY_HOSTPORT, "-1"));
		if (port != -1) {
			if (port == 80 && isHttps(mPrefs)) {
				port = 443;
			}
		}
		if (port != -1) {
			sb.append(":").append(port);
		}
		sb.append("/")
			.append(mPrefs.getString(MainActivity.KEY_HOSTPATH, "todorest/rs"))
			.append(finalPath);
		try {
			return new URL(sb.toString());
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Messed up URL: " + e, e);
		}
	}

	private void synchSaveOrUpdateLocalTasks(final List<AndroidTask> remote)
			throws IOException, JsonParseException, JsonMappingException {
		for (AndroidTask t : remote) {
			System.out.println(t);
			if (t.get_Id() > 0) {
				mDao.update(t);
			} else {
				long _id = mDao.insert(t);
				t.set_Id(_id);
			}	
			Log.d(TAG, "Downloaded and inserted this new Task: " + t);
		}
	}

	/**
	 * Send the local tasks that are new or modified.
	 * @param toSend The List of local tasks to be sent to the central server.
	 * @throws JsonProcessingException On error
	 * @throws UnsupportedEncodingException On error
	 * @throws IOException On error
	 * @throws ClientProtocolException On error
	 * @throws URISyntaxException  On error
	 */
	private void synchSaveOrUpdateRemoteTasks(final List<AndroidTask> toSend)
			throws JsonProcessingException, UnsupportedEncodingException, IOException, ClientProtocolException, URISyntaxException {

		String proto = isHttps(mPrefs) ? "https" : "http";
		int portNum = Integer.parseInt(mPrefs.getString(MainActivity.KEY_HOSTPORT, "-1"));
		if (portNum == -1) { // not manually set
			portNum = isHttps(mPrefs) ? 443 : 80;
		}
		// /{userName}/task/new
		final URL postUriNew = new URL(String.format("%s://%s:%d/%s/%s/task/new",
				proto,
				mPrefs.getString(MainActivity.KEY_HOSTNAME, "10.0.2.2"),
				portNum,
				pathStr.startsWith("/") ? pathStr.substring(1) : pathStr,
				"iadmin"));
		Log.d(TAG, "Connecting to server for " + postUriNew);

		Map<String,String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("Accept", "text/plain");
		//Context c = getContext().getApplicationContext();
		headers.put("Authorization", TodoMoreApplication.makeBasicAuthString());
		
		// Send each task in the list
		for (AndroidTask at : toSend) {

			final ObjectWriter w = jacksonMapper.writer();
			String json = w
					.with(SerializationFeature.INDENT_OUTPUT)
					.without(SerializationFeature.WRAP_EXCEPTIONS)
					.writeValueAsString(at);

			// INVOKE
			String response = UrlConnector.converse(postUriNew, json, headers);

			System.out.println("POST response: " + response);
			
			// Get the "created" response body from the response

			final String resultStr = response;

			// it actually sends the URL of the new ID
			Uri resultUri = Uri.parse(resultStr);
			long id = ContentUris.parseId(resultUri);
			at.setId(id);
			if (mDao.update(at)) {
				Log.e(TAG, "FAILED TO UPDATE");
			}
			Log.d(TAG, "UPDATED " + at + ", new  Remote ID = " + at.getId());
		}
	}
	
	public int getPort() {
		final int prefsValue = Integer.parseInt(mPrefs.getString(MainActivity.KEY_HOSTPORT, "-1"));
		if (prefsValue == 80 && isHttps(mPrefs))
			return /* override port# pref with "use https" checkbox */ 443;
		if (prefsValue == -1) {
			return isHttps(mPrefs) ? 443 : 80;
		}
		return prefsValue;
	}
	
	public String getPath() {
		return mPrefs.getString(MainActivity.KEY_HOSTPATH, getContext().getString(R.string.default_host_path));
	}

	// Step 0
	private void syncRunDeleteQueue() {
		// TODO
	}

	// Step 1
	private List<AndroidTask> syncGetTasksFromRemote(HttpClient client) throws Exception {

		pathStr = mPrefs.getString(MainActivity.KEY_HOSTPATH, "/");
		String proto = isHttps(mPrefs) ? "https" : "http";
		URI getUri = new URI(String.format("%s://%s:%d/%s/%s/tasks",
				proto,
				mPrefs.getString(MainActivity.KEY_HOSTNAME, null),
				getPort(),
				pathStr.startsWith("/") ? pathStr.substring(1) : pathStr, mPrefs.getString(MainActivity.KEY_USERNAME, null)));
		Log.d(TAG, "Getting Items From " + getUri);
		HttpGet httpAccessor = new HttpGet();
		httpAccessor.setURI(getUri);
		httpAccessor.addHeader("Content-Type", "application/json");
		httpAccessor.addHeader("Accept", "application/json");
		HttpResponse getResponse = client.execute(httpAccessor);	// CONNECT
		final HttpEntity getResults = getResponse.getEntity();
		final String tasksStr = EntityUtils.toString(getResults);
		List<AndroidTask> newToDos = jacksonMapper.readValue(tasksStr, new TypeReference<List<AndroidTask>>(){});
		Log.d(TAG, "Done Getting Items, list size = " + newToDos.size());
		return newToDos;
	}
}

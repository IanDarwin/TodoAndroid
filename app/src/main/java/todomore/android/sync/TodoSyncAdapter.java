package todomore.android.sync;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.*;
import java.time.chrono.ChronoLocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.darwinsys.todo.model.Task;
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

	private final static String TAG = TodoSyncAdapter.class.getSimpleName();

	private final static String LAST_SYNC_TSTAMP = "last sync";
	private LocalDateTime lastSynchTime = LocalDateTime.MIN;

	private SharedPreferences mPrefs;
	private TaskDao mDao;

	private ObjectMapper jacksonMapper = new ObjectMapper();
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
			return false;
		}
		for (String k : keys) {
			if (mPrefs.getString(k, null) == null) {
				System.out.println("TodoSyncAdapter.isSynchEnabled(): FAIL ON S " + k);
				return false;
			};
		}
		return true;
	}
	
	public static boolean isHttps(SharedPreferences mPrefs) {
		return mPrefs.getBoolean(MainActivity.KEY_HOST_HTTPS, true);
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

		if (mPrefs.getString(LAST_SYNC_TSTAMP, null) != null)
			lastSynchTime = LocalDateTime.parse(mPrefs.getString(LAST_SYNC_TSTAMP, null));
		ArrayList<Task> toSaveRemotely = new ArrayList<Task>();
		ArrayList<Task> toSaveLocally = new ArrayList<Task>();

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

		try {
			// Zeroeth, delete any remote items we previously deleted locally;
			// must precede all else, so we don't get zombies appearing!
			syncRunDeleteQueue();

			// First, get list of items FROM the local DB and the remote server
			final List<Task> local = mDao.findAll();
			final List<Task> remote = syncGetTasksFromRemote();

			// NOW RUN LOGIC TO FIGURE OUT WHAT TO PUT WHERE
			TodoSyncAdapter.algorithm(local, remote, 
					lastSynchTime,
					toSaveLocally, toSaveRemotely);
			
			synchSaveOrUpdateLocalTasks(toSaveLocally);

			synchSaveOrUpdateRemoteTasks(toSaveRemotely);

			// Finally, update our synch timestamp!
			mPrefs.edit().putLong(LAST_SYNC_TSTAMP, System.currentTimeMillis()).commit();

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
	 * @param lastSynchDate  Completion time viewed locally when last synch operation finished.
	 * @param toSaveLocally  The list of Tasks to be saved (added OR updated) locally
	 * @param toSaveRemotely The list of Tasks to be saved (added OR updated) remotely
	 * NOTparam toDeleteRemotely The list of Tasks to be deleted locally.
	 */
	public static void algorithm(List<Task> local, List<Task> remote, 
			LocalDateTime lastSynchDate,
			List<Task> toSaveLocally, List<Task> toSaveRemotely) {

		List<Task> toDeleteLocally = new ArrayList<>();

		// First, if local is empty and remote is not, assume we just installed
		// on a new device, install everything locally.
		if (local.isEmpty() && !remote.isEmpty()) {
			toSaveLocally.addAll(remote);
			return;
		}
		
		// The same if remote is empty and local is not.
		if (!local.isEmpty() && remote.isEmpty()) {
			toSaveRemotely.addAll(local);
			return;
		}

		// Compute the list of local tasks that must be sent to the server.
		// In same pass, pre-compute list of entries in local DB that have remote ids.
		List<Long> localsWithServerId = new ArrayList<>();
		for (Task t : local) {
			if (t.getServerId() != 0) {
				localsWithServerId.add(t.getServerId());
			}
			if (t.getServerId() == 0 || (t.getModified().isAfter(ChronoLocalDate.from(lastSynchDate)))) {
				toSaveRemotely.add(t);
				continue;
			}
		}
		
		// Compute the list of remote tasks that must be saved/updated OR DELETED locally
		for (Task t : remote) {
			// Objects in list "remote" cannot have a deviceId yet
			long serverId = t.getServerId();
			if (!localsWithServerId.contains(serverId)) { // Not in our local DB
				// If remote object created before lastSync and not in local DB,
				// then we know it got deleted on the server (by web site or user's other mobile)
				 if (t.getCreationDate().isBefore(ChronoLocalDate.from(lastSynchDate))) {
				 	toDeleteLocally.add(t);
					continue;
				 }
				toSaveLocally.add(t);	// it's newly created remotely
				continue;
			}
			if (t.getModified().isAfter(ChronoLocalDate.from(lastSynchDate))) {
				toSaveLocally.add(t);	// remote version was modified after last synch
				continue;
			}
		}
		System.out.print("SYNCH: TO-DELETE-LOCALLY: ");
		for (Task t : toDeleteLocally) {
			System.out.println(t.getDeviceId() + "--" + t.getName());
		}
		System.out.println();
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
			// XXX default value for HOSTPATH should be gotten from XML defaults file
			.append(mPrefs.getString(MainActivity.KEY_HOSTPATH, "todorest/todo"))
			.append(finalPath);
		try {
			return new URL(sb.toString());
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Messed up URL: " + e, e);
		}
	}

	private void synchSaveOrUpdateLocalTasks(final List<Task> tasks)
			throws IOException, JsonParseException, JsonMappingException {
		for (Task t : tasks) {
			System.out.println(t);
			if (t.getDeviceId() != null) {
				mDao.update(t);
			} else {
				mDao.insert(t); // sets deviceId in object for us
			}	
			Log.d(TAG, "Saved/Updated this Task: " + t);
		}
	}

	/**
	 * Send the local tasks that are new or modified.
	 * @param toSend The List of local tasks to be sent to the central server.
	 * @throws JsonProcessingException On error
	 * @throws UnsupportedEncodingException On error
	 * @throws IOException On error
	 * @throws URISyntaxException  On error
	 */
	private void synchSaveOrUpdateRemoteTasks(final List<Task> toSend)
			throws Exception {

		String proto = isHttps(mPrefs) ? "https" : "http";
		int portNum = Integer.parseInt(mPrefs.getString(MainActivity.KEY_HOSTPORT, "-1"));
		if (portNum == -1) { // not manually set
			portNum = isHttps(mPrefs) ? 443 : 80;
		}
		// /{userName}/task/save
		final URL postUriNew = new URL(String.format("%s://%s:%d/%s/%s/task/save",
				proto,
				mPrefs.getString(MainActivity.KEY_HOSTNAME, "10.0.2.2"),
				portNum,
				pathStr.startsWith("/") ? pathStr.substring(1) : pathStr,
				mPrefs.getString(MainActivity.KEY_USERNAME, "USERNAME NOT SET!?")));
		Log.d(TAG, "Connecting to server for " + postUriNew);

		Map<String,String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("Accept", "text/plain");
		headers.put("Authorization", TodoMoreApplication.makeBasicAuthString());
		
		// Send each task in the list
		for (Task t : toSend) {

			final ObjectWriter w = jacksonMapper.writer();
			String json = w
					.with(SerializationFeature.INDENT_OUTPUT)
					.without(SerializationFeature.WRAP_EXCEPTIONS)
					.writeValueAsString(t);

			// INVOKE
			String response = UrlConnector.converse(postUriNew, json, headers);
			
			// Hopefully get a "created" response body from the response
			System.out.println("POST response: " + response);

			final String resultStr = response;

			// it actually sends the URL of the new ID
			Uri resultUri = Uri.parse(resultStr);
			long id = ContentUris.parseId(resultUri);
			t.setServerId(id);
			if (!mDao.update(t)) {
				Log.e(TAG, "FAILED TO UPDATE");
			}
			Log.d(TAG, "UPDATED " + t + ", new  Remote ID = " + t.getServerId());
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
	private void syncRunDeleteQueue() throws MalformedURLException {
		long[] deletions = mDao.findDeletions();
		if (deletions.length == 0) {
			// Common, no local deletions yet or since last sync.
			return;
		}

		Map<String,String> headers = new HashMap<>();
		headers.put("Accept", "text/plain");
		headers.put("Content-type", "application/json");
		headers.put("Authorization", TodoMoreApplication.makeBasicAuthString());
		
		pathStr = mPrefs.getString(MainActivity.KEY_HOSTPATH, "/");
		URL url = new URL(String.format("%s://%s:%d/%s/%s/task/smersh",
				isHttps(mPrefs) ? "https" : "http",
				mPrefs.getString(MainActivity.KEY_HOSTNAME, null),
				getPort(),
				pathStr.startsWith("/") ? pathStr.substring(1) : pathStr, 
				mPrefs.getString(MainActivity.KEY_USERNAME, null)));
		
		for (long id : deletions) {
			UrlConnector.converse(url,
				// Only a bare-naked-minimum "Task" object is needed here:
				String.format("{\"serverId\":%d}", id), headers);
			// Nothing to do - it will throw an exception if it fails.
			mDao.deleteDeletion(id);
		}
	}

	// Step 1
	private List<Task> syncGetTasksFromRemote() throws Exception {

		Map<String,String> headers = new HashMap<>();
		headers.put("Accept", "application/json");
		headers.put("Authorization", TodoMoreApplication.makeBasicAuthString());
		
		pathStr = mPrefs.getString(MainActivity.KEY_HOSTPATH, "/");
		URL getUri = new URL(String.format("%s://%s:%d/%s/%s/tasks",
				isHttps(mPrefs) ? "https" : "http",
				mPrefs.getString(MainActivity.KEY_HOSTNAME, null),
				getPort(),
				pathStr.startsWith("/") ? pathStr.substring(1) : pathStr, 
				mPrefs.getString(MainActivity.KEY_USERNAME, null)));
		Log.d(TAG, "Getting Items From " + getUri);
		String tasksStr = UrlConnector.converse(getUri, null, headers);
		List<Task> newToDos = jacksonMapper.readValue(tasksStr, new TypeReference<List<Task>>(){});
		Log.d(TAG, "Done Getting Items, list size = " + newToDos.size());
		return newToDos;
	}
}

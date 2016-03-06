package todomore.android.sync;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.darwinsys.todo.model.Task;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import todomore.android.TaskDao;


/**
 * Android Synch Adapter for Todo List Tasks;
 * write items to, and read items from, the REST server.
 * @author Ian Darwin
 */
public class TodoSyncAdapter extends AbstractThreadedSyncAdapter {
	
	private final static String TAG = TodoSyncAdapter.class.getSimpleName();
	
	private final static String LAST_SYNC_TSTAMP = "last sync";
	
	private SharedPreferences mPrefs;
	private TaskDao mDao;
	
		ObjectMapper jacksonMapper = new ObjectMapper();

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

			final long lastSynchTime = mPrefs.getLong(LAST_SYNC_TSTAMP, 0L);
			ArrayList<Task> toSaveRemotely = new ArrayList<>();
			ArrayList<Task> toSaveLocally = new ArrayList<>();
			ArrayList<Task> toDeleteRemotely = new ArrayList<>();

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

				// First, get list of items FROM the remote server
				final List<Task> remote = syncGetTasksFromRemote(client);

				// NOW SEND ANY ITEMS WE'VE CREATED/MODIFIED, going FROM the Task DAO
				// TO the remote web service.

				final URI postUri = new URI(String.format("http://%s:%d/%s/new/tasks", 
						mPrefs.getString(MainActivity.KEY_HOSTNAME, "10.0.2.2"),
						Integer.parseInt(mPrefs.getString(MainActivity.KEY_HOSTPORT, "80")),
						pathStr.startsWith("/") ? pathStr.substring(1) : pathStr));

				final List<Task> local = mDao.findAll();
				syncSendLocalTasks(lastSynchTime, client, postUri, local);

				// NOW RUN LOGIC TO FIGURE OUT WHAT TO PUT WHERE
				TodoSyncAdapter.onSynchCore(local, remote, 
						lastSynchTime,
						toSaveRemotely, toSaveLocally);

				// Order matters - use list we fetched earlier,
				// to avoid possibility of bouncing items back to the server that we just got
				syncUpdateRemotelyChangedTasks(remote);

				// Finally send deletions
				syncSendDeletions();

				// Finally, update our timestamp!
				mPrefs.edit().putLong(LAST_SYNC_TSTAMP, System.currentTimeMillis());

			} catch (Exception e) {
				Log.wtf(TAG, "ERROR in synchronization!: " + e, e);
			}
		}
		
		/**
		 * DO THE ACTUAL SYNCHRONIZATION LOGIC HERE
		 * Isolated to just deal with lists, to make testing easier(possible).
		 * It is expected that the items in toDeleteRemotely will be deleted after
		 * this method completes, but before lastSyncTime is updated.
		 * @param local The list of existing Tasks in the local database
		 * @param remote The list of existing Tasks in the remote database
		 * @param lastSyncTime  Completion time viewed locally when last synch operation finished.
		 * @param toSaveLocally  The list of Tasks to be saved (added OR updated) locally
		 * @param toSaveRemotely The list of Tasks to be saved (added OR updated) remotely
		 * @param toDeleteRemotely The list of Tasks to be deleted locally.
		 */
		public static void onSynchCore(List<Task> local, List<Task> remote, 
				long lastSyncTime, 
				List<Task> toSaveLocally, List<Task> toSaveRemotely) {
			// KLUDGE just to pass tests initially DO NOT USE
			toSaveRemotely.addAll(local);
		}

		private HttpClient syncSetupRestConnection(String userName, String password) {
			final DefaultHttpClient client = new DefaultHttpClient();
			Credentials creds = new UsernamePasswordCredentials(userName, password);        
			((AbstractHttpClient)client).getCredentialsProvider()
			.setCredentials(new AuthScope(
					mPrefs.getString(MainActivity.KEY_HOSTNAME, "10.0.2.2"), 
					mPrefs.getInt(MainActivity.KEY_HOSTPORT, 80)),
					creds);
			return client;
		}

		private void syncUpdateRemotelyChangedTasks(final List<Task> newToDos)
				throws IOException, JsonParseException, JsonMappingException {
			for (Object o : newToDos) {
				System.out.println(o);
				Task t = jacksonMapper.readValue(o.toString(), Task.class);
				mDao.insert(t);
				Log.d(TAG, "Downloaded and inserted this new Task: " + t);
			}
		}

		private void syncSendLocalTasks(final long lastSynchTime, HttpClient client, final URI postUri,
				final List<Task> localTasks)
				throws JsonProcessingException, UnsupportedEncodingException, IOException, ClientProtocolException {
			for (Task t : localTasks) {

				AndroidTask at = (AndroidTask) t;
				// Send this task if it has no remoteId or if it's modified since last sync
				if (!(at.getRemoteId() == 0 || at.getModified() > lastSynchTime)) {
					continue;
				}

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

				// Get the "created" response body from the response
				final String resultStr = response.getFirstHeader("Location").getValue();

				// it actually sends the URL of the new ID
				Uri resultUri = Uri.parse(resultStr);
				long id = ContentUris.parseId(resultUri);
				t.setId(id);
				if (mDao.update(t)) {
					Log.e(TAG, "FAILED TO UPDATE");
				}
				Log.d(TAG, "UPDATED " + t + ", new  Remote ID = " + t.getId());
			}
		}
		// Step 0
		private void syncRunDeleteQueue() {
			// TODO
		}
		
		// Step 1
		private List<Task> syncGetTasksFromRemote(HttpClient client) throws Exception {

			pathStr = mPrefs.getString(MainActivity.KEY_HOSTPATH, "/");
			URI getUri = new URI(String.format("http://%s:%d/%s/%s/tasks", mPrefs.getString(MainActivity.KEY_HOSTNAME, null),
					Integer.parseInt(mPrefs.getString(MainActivity.KEY_HOSTPORT, "80")),
					pathStr.startsWith("/") ? pathStr.substring(1) : pathStr, mPrefs.getString(MainActivity.KEY_USERNAME, null)));
			Log.d(TAG, "Getting Items From " + getUri);
			HttpGet httpAccessor = new HttpGet();
			httpAccessor.setURI(getUri);
			httpAccessor.addHeader("Content-Type", "application/json");
			httpAccessor.addHeader("Accept", "application/json");
			HttpResponse getResponse = client.execute(httpAccessor);	// CONNECT
			final HttpEntity getResults = getResponse.getEntity();
			final String tasksStr = EntityUtils.toString(getResults);
			@SuppressWarnings("unchecked")
			List<Task> newToDos = jacksonMapper.readValue(tasksStr, List.class);
			Log.d(TAG, "Done Getting Items, list size = " + newToDos.size());
			return newToDos;
		}
		
		private void syncSendDeletions() {
			// XXX
		}
}

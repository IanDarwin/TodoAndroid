package todomore.android;

import java.net.URI;
import java.util.List;

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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.darwinsys.todo.model.Task;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;


/**
 * Android Synch Adapter for Todo List Tasks;
 * write items to, and read items from, the REST server.
 * @author Ian Darwin
 */
public class TodoSyncAdapter extends AbstractThreadedSyncAdapter {
	
	private final static String TAG = TodoSyncAdapter.class.getSimpleName();
	
	private final static String LAST_SYNC_TSTAMP = "last sync";
	
	private final ContentResolver mResolver;
	private SharedPreferences mPrefs;
	private TaskDao mDao;
	
		ObjectMapper jacksonMapper = new ObjectMapper();
		
		public TodoSyncAdapter(Context context, boolean autoInitialize) {
			this(context, autoInitialize, false);
		}

		public TodoSyncAdapter(
				Context context,
				boolean autoInitialize,
				boolean allowParallelSyncs) {
			super(context, autoInitialize, allowParallelSyncs);

			mResolver = context.getContentResolver();
			mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
			mDao = new TaskDao(context);
		}

		@Override
		public void onPerformSync(Account account, 
				Bundle extras, 
				String authority,
				ContentProviderClient provider, 
				SyncResult syncResult) {
			Log.d(TAG, "ToDoSyncAdapter.onPerformSync()");
			
			// Get the username and password, set there by our LoginActivity.
			
			long tStamp = mPrefs.getLong(LAST_SYNC_TSTAMP, 0L);
			
			AppSingleton app = AppSingleton.getInstance();
			String userName = app.getUserName();
			String password = app.getPassword();
			Log.d(TAG, "Starting TODO Sync for " + userName);
			
			HttpClient client = new DefaultHttpClient();
			Credentials creds = new UsernamePasswordCredentials(userName, password);        
			((AbstractHttpClient)client).getCredentialsProvider()
				.setCredentials(new AuthScope(RestConstants.SERVER, RestConstants.PORT), creds); 
			
			// First, get list of items modified on the server
			try {
			final URI getUri = new URI(String.format(RestConstants.PROTO + "://%s:%d/%s/%s/tasks", 
					RestConstants.SERVER, RestConstants.PORT, RestConstants.PATH_PREFIX, userName));
			Log.d(TAG, "Getting Items From " + getUri);
			HttpGet httpAccessor = new HttpGet();
			httpAccessor.setURI(getUri);
			httpAccessor.addHeader("Content-Type", "application/json");
			httpAccessor.addHeader("Accept", "application/json");
			HttpResponse getResponse = client.execute(httpAccessor);	// CONNECT
			final HttpEntity getResults = getResponse.getEntity();
			final String tasksStr = EntityUtils.toString(getResults);
			Log.d(TAG, "JSON list string is: " + tasksStr);
			List<?> newToDos = jacksonMapper.readValue(tasksStr, List.class);
			Log.d(TAG, "Done Getting Items, list size = " + newToDos.size());
		
			// NOW SEND ANY ITEMS WE'VE CREATED/MODIFIED, going FROM the ContentResolver
			// TO the remote sync server.

			final URI postUri = new URI(String.format(RestConstants.PROTO + "://%s/todo/%s/tasks", RestConstants.PATH_PREFIX, userName));
			String sqlQuery = "modified < ?";
			for (Task t : mDao.findAll()) {

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

				// it actually sends the URL of the new ID
				Uri resultUri = Uri.parse(resultStr);
				long id = ContentUris.parseId(resultUri);
				t.setId(id);;
				if (mDao.update(t)) {
					Log.e(TAG, "FAILED TO UPDATE");
				}
				Log.d(TAG, "UPDATED " + t + ", new _ID = " + t.getId());
			}
			
			// NOW GET ONES UPDATED ON THE SERVER
			
			// Order matters to avoid possibility of bouncing items back to the server that we just got

			for (Object o : newToDos) {
				System.out.println(o);
				Task t = jacksonMapper.readValue(o.toString(), Task.class);
			mDao.insert(t);
			Log.d(TAG, "Downloaded and inserted this new Task: " + t);
		}
	
	} catch (Exception e) {
		Log.wtf(TAG, "ERROR in synchronization!: " + e, e);
	}
}
}

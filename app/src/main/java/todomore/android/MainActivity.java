package todomore.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.Toast;

import com.darwinsys.todo.model.Priority;
import com.darwinsys.todo.model.Status;
import com.darwinsys.todo.model.Task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements SearchView.OnQueryTextListener {

	public static final String TAG = MainActivity.class.getName();
	private EditText addTF;
	private Spinner prioSpinner;
	private SearchView mSearchView;
	private ListView mListView;
	private int ACTIVITY_ID_LOGIN;
	private static SharedPreferences mPrefs;
	
	// Keys for mPrefs lookups - **MUST** be in "sync" with values/keys.xml!
	// But must be initted here cuz used statically from outside app by SyncManager,
	// and our onCreate() will not have been called.
	public static final String KEY_USERNAME = "KEY_USERNAME";
	public static String KEY_PASSWORD = "KEY_PASSWORD";
	public static String KEY_HOSTNAME = "KEY_HOSTNAME";
	public static String KEY_HOSTPORT = "KEY_HOSTPORT";
	public static String KEY_HOSTPATH = "KEY_HOSTPATH";
	public static String KEY_HOST_HTTPS = "KEY_HOST_HTTPS";
	public static String KEY_ENABLE_SYNCH = "KEY_ENABLE_SYNCH";
	public static String KEY_SYNC_INTERVAL = "KEY_SYNC_INTERVAL";
	public static String KEY_SHOW_COMPLETED = "KEY_SHOW_COMPLETED";

	/** The account name */
	public static final String ACCOUNT = "account";
	/* The account */
	private static Account mAccount;
	
	// Data
	List<String> fullTitlesList;
	List<Task> fullTaskList;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		addTF = (EditText) findViewById(R.id.addTF);
		mListView = (ListView) findViewById(R.id.listView);
		mListView.setOnItemClickListener((parent, view, position, id) -> {
			Task task = fullTaskList.get(position);
			long _id = task.getDeviceId();
			Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
			intent.putExtra("taskId", _id);
			startActivity(intent);
		});

		ArrayAdapter<CharSequence> adapter = 
			ArrayAdapter.createFromResource(this, R.array.priorities_array,
				android.R.layout.simple_spinner_item);
		
		// Tailor the adapter for the SearchView
		mListView.setTextFilterEnabled(true);
		
		mSearchView = (SearchView) findViewById(R.id.searchView);
		mSearchView.setIconifiedByDefault(false);
		mSearchView.setOnQueryTextListener(this);
		mSearchView.setSubmitButtonEnabled(false);
		mSearchView.setQueryHint(getString(R.string.search_hint));
		
		// Specify the layout to use when the list of choices appears
		prioSpinner = (Spinner) findViewById(R.id.prioSpinner);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		prioSpinner.setAdapter(adapter);
		prioSpinner.setSelection(Priority.High.ordinal());

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		mAccount = createSyncAccount(this);
		enableSynching(mPrefs.getBoolean(KEY_ENABLE_SYNCH, true));
		
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
		if (enable) {
			ContentResolver.setSyncAutomatically(mAccount, authority, true);
			// Force immediate - will probably remove this later
			Bundle immedExtras = new Bundle();
			immedExtras.putBoolean("SYNC_EXTRAS_MANUAL", true);
			ContentResolver.requestSync(mAccount, authority, immedExtras);

			// Request periodic synching - interval is in seconds NOT mSec.
			Bundle extras = new Bundle();
			long pollFrequency = Long.parseLong(mPrefs.getString(KEY_SYNC_INTERVAL, "60")) * 60;
			ContentResolver.addPeriodicSync(mAccount, authority, extras, pollFrequency);
		} else {
			// Disabling, so cancel all outstanding syncs until further notice
			ContentResolver.cancelSync(mAccount, authority);
		}
	}
	/**
	 * Create a new dummy account for the sync adapter.
	 * @author Adapted from http://developer.android.com/ page on this topic.
	 * @param context The application context
	 */
	Account createSyncAccount(Context context) {
		// Get the Android account manager
		AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
		Account[] accounts = accountManager.getAccountsByType(
				getString(R.string.accountType));
		if (accounts.length == 0) {
			// Create the account type and default account
			Account newAccount = new Account(ACCOUNT, getString(R.string.accountType));
			/*
			 * Add the account and account type, no password or user data yet.
			 * If successful, return the Account object, otherwise report an error.
			 */
			if (accountManager.addAccountExplicitly(newAccount, "top secret", null)) {
				Log.d(TAG, "Add Account Explicitly: Successfully");
				return newAccount;
			} else {
				throw new IllegalStateException("Add Account Explicitly failed...");
			}
		} else {
			return accounts[0];
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		addTF.requestFocus();
		loadListFromDB();
	}
	
	private void loadListFromDB() {
		boolean showCompleted = mPrefs.getBoolean(KEY_SHOW_COMPLETED, false);
		fullTaskList = showCompleted?
				((TodoMoreApplication)getApplication()).getTaskDao().findAll() :
				((TodoMoreApplication)getApplication()).getTaskDao().findUncompleted();
		fullTitlesList = new ArrayList<>();
		for (Task t : fullTaskList) {
			fullTitlesList.add(t.getName());
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<>(
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
		if (name.isEmpty()) {
			Toast.makeText(this, "Text required!", Toast.LENGTH_SHORT).show();
			return;
		}

		// Do the work here! Save to local DB, let Sync Adapter send it to the server...
		Task t = new Task();
		t.setName(addTF.getText().toString());
		t.setPriority(Priority.values()[prioSpinner.getSelectedItemPosition()]);
		t.setModified(LocalDate.now());
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
	
	// Methods for the SearchView-ListView tie-in
	
	public boolean onQueryTextChange(String newText) {
		if (TextUtils.isEmpty(newText)) {
			mListView.clearTextFilter();
		} else {
			mListView.setFilterText(newText);
		}
		return true;
	}
	public boolean onQueryTextSubmit(String query) {
		return false;
	}
}

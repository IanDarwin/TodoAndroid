package todomore.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import todomore.android.sync.TodoSyncAdapter;

/**
 * Tests the Preferences helpers both in MainActivity and in TodoSyncAdapter
 * @author Ian Darwin
 */
@RunWith(RobolectricTestRunner.class)
public class PreferencesHelpersTest {

	private SharedPreferences mPrefs;
	private MainActivity activity;
	private TodoSyncAdapter adapter;

	@Before
	public void setUp() throws Exception {
		activity = Robolectric.buildActivity(MainActivity.class).create().get();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(new MainActivity());
		adapter = new TodoSyncAdapter(activity, mPrefs, false);
	}

	@Test
	public void testPrefsSettings() {
		final Editor editor = mPrefs.edit();
		editor.putBoolean(MainActivity.KEY_ENABLE_SYNCH, true);
		editor.putBoolean(MainActivity.KEY_HOST_HTTPS, true);
		// These 4 must also be set for synching to be enabled.
		editor.putString(MainActivity.KEY_HOSTNAME, "abcdefg");
		editor.putString(MainActivity.KEY_HOSTPATH, "abcdefg");
		editor.putString(MainActivity.KEY_USERNAME, "abcdefg");
		editor.putString(MainActivity.KEY_PASSWORD, "abcdefg");
		editor.commit();
		assertTrue(adapter.isHttps(mPrefs));
		assertTrue(adapter.isSynchEnabled(mPrefs));
		// With https=true and no port, The number shalle be 443
		assertEquals(443, adapter.getPort());
		mPrefs.edit().putString(MainActivity.KEY_HOSTPORT, Integer.toString(80)).commit();
		// With https=true and port set to 80, port should flip to 443
		assertEquals(443, adapter.getPort());
		mPrefs.edit().putString(MainActivity.KEY_HOSTPORT, Integer.toString(99)).commit();
		// With https=true and port set, port should remain unchanged
		assertEquals(99, adapter.getPort());
	}

}

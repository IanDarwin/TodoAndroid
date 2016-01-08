package todomore.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


/**
 * A preferences screen that offers username/password and other settings
 */
public class PrefsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set up defaults before creating PrefsFragment
        PreferenceManager.setDefaultValues(this, R.layout.prefs, false);
        
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
    
    private void testNetworkSetup() {
		Toast.makeText(this, "Test not written yet", Toast.LENGTH_SHORT).show();
	}

	public static class PrefsFragment extends PreferenceFragment {
    	
    	public void onCreate(Bundle savedInstanceState) {
    		super.onCreate(savedInstanceState);
    		addPreferencesFromResource(R.layout.prefs);
    	}
    }
}




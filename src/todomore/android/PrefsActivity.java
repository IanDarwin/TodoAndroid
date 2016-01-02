package todomore.android;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;


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
    
    public static class PrefsFragment extends PreferenceFragment {
    	
    	public void onCreate(Bundle savedInstanceState) {
    		super.onCreate(savedInstanceState);
    		addPreferencesFromResource(R.layout.prefs);
    	}
    }

}




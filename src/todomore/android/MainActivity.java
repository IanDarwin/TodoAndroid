package todomore.android;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

public class MainActivity extends Activity {
	static String TAG = MainActivity.class.getSimpleName();
	EditText addTF;
	Spinner prioSpinner;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        addTF = (EditText) findViewById(R.id.addTF);
        prioSpinner = (Spinner) findViewById(R.id.prioSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.priorities_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        prioSpinner.setAdapter(adapter);
    }
    
    /** Called from the View when the Add button is pressed */
    public void addItem(View v) {
    	String name = addTF.getText().toString();
    	Log.d(TAG, "addItem: " + name);
    	if (name == null || name.length() == 0) {
    		Toast.makeText(this, "Text required!", Toast.LENGTH_SHORT).show();
    		return;
    	}
    	// If we get here, remove text so it doesn't get added twice
    	addTF.setText("");
    	Toast.makeText(this, "(Fake) Done", Toast.LENGTH_SHORT).show();
    }
}
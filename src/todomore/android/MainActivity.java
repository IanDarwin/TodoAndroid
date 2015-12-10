package todomore.android;

import com.darwinsys.todo.model.Priority;
import com.darwinsys.todo.model.Task;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class MainActivity extends Activity {
	static String TAG = MainActivity.class.getSimpleName();
	EditText addTF;
	Spinner prioSpinner;
	
	TaskDao mDao;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        addTF = (EditText) findViewById(R.id.addTF);
        prioSpinner = (Spinner) findViewById(R.id.prioSpinner);
        mDao = new TaskDao(this);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.priorities_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        prioSpinner.setAdapter(adapter);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.main_menu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.settings_menuitem:
    		startActivity(new Intent(this, LoginActivity.class));
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
    	if (name == null || name.length() == 0) {
    		Toast.makeText(this, "Text required!", Toast.LENGTH_SHORT).show();
    		return;
    	}
    	
    	// Do the work here! Save to local DB, send it to the server...
    	Task t = new Task();
    	t.setName(addTF.getText().toString());
    	t.setPriority(Priority.values()[prioSpinner.getSelectedItemPosition()]);
    	t.setModified(System.currentTimeMillis());
    	
    	mDao.insert(t);
    	
    	// XXX Send to server, or, trigger sync?
    	
    	// If we get here, remove text so it doesn't get added twice
    	addTF.setText("");
    	Toast.makeText(this, "Saved locally", Toast.LENGTH_SHORT).show();
    }
}
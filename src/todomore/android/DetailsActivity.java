package todomore.android;

import org.metawidget.android.widget.AndroidMetawidget;
import org.metawidget.android.widget.widgetprocessor.binding.simple.SimpleBindingProcessor;

import com.darwinsys.todo.model.Task;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/** 
 * An Activity that shows all the Details of one Task, using MetaWidget.
 * It starts in display mode, but an Edit button makes it editable.
 * @author Ian Darwin
 */
public class DetailsActivity extends Activity {

	private static final String TAG = DetailsActivity.class.getSimpleName();
	private Task mTask;
	private AndroidMetawidget mMetawidget;

	public void onCreate(Bundle saved) {
		super.onCreate(saved);
		setContentView(R.layout.details_activity);

		long id = getIntent().getLongExtra("taskId", -1);
		Log.d(TAG, "_id = " + id);
		if (id != -1) {
			mTask = ((TodoMoreApplication) getApplication()).getTaskDao().findById(id);
		} else {
			mTask = new AndroidTask();
		}

		mMetawidget = (AndroidMetawidget) findViewById(R.id.metawidget);
        mMetawidget.setToInspect(mTask);
        if (id == -1) {
            mMetawidget.setReadOnly(false);
        }

        if (!mMetawidget.isReadOnly()) {
            enableEditing();
        }
        
        Button enableEditButton = (Button) findViewById(R.id.enableEditButton);
        enableEditButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Log.d(TAG, "onClick");
				enableEditing();
			}
		});
	}
	
	private final View.OnClickListener saver = new View.OnClickListener() {
		public void onClick(View v) {
			Toast.makeText(DetailsActivity.this, "Saving...", Toast.LENGTH_SHORT).show();
			
			// Get the fields from the metawidget subfields into mTask:
			mMetawidget.getWidgetProcessor(SimpleBindingProcessor.class).save( mMetawidget );// XXX something with a DAO

			// Do the actual save
			((TodoMoreApplication) getApplication()).getTaskDao().update(mTask);
			
			// If no exception thrown...
			finish();
		}
	};

	private void enableEditing() {
		Log.d(TAG, "enableEditing");
		mMetawidget.setReadOnly(false);
		mMetawidget.buildWidgets();
		View view = mMetawidget.findViewWithTag("name");
		view.setFocusable(true);
		view.setFocusableInTouchMode(true);
		view.requestFocus();
		// And the Save button
		Button save = (Button) findViewById(R.id.saveButton);
		save.setVisibility(View.VISIBLE);
		save.setOnClickListener(saver);
	}
}

package todomore.android;

import org.metawidget.android.widget.AndroidMetawidget;
import org.metawidget.android.widget.widgetbuilder.AndroidWidgetBuilder;
import org.metawidget.android.widget.widgetbuilder.OverriddenWidgetBuilder;
import org.metawidget.android.widget.widgetbuilder.ReadOnlyWidgetBuilder;
import org.metawidget.android.widget.widgetprocessor.binding.simple.SimpleBindingProcessor;
import org.metawidget.widgetbuilder.composite.CompositeWidgetBuilder;
import org.metawidget.widgetbuilder.composite.CompositeWidgetBuilderConfig;

import com.darwinsys.todo.model.Task;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import todomore.android.metawidget.TodoMoreAndroidWidgetBuilder;

/** 
 * An Activity that shows all the Details of one Task, using MetaWidget.
 * It starts in display mode, but an Edit button makes it editable.
 * @author Ian Darwin
 */
public class DetailsActivity extends Activity {

	private static final String TAG = DetailsActivity.class.getSimpleName();
	private Task mTask;
	private AndroidMetawidget mMetawidget;
	private Button enableEditButton;

	public void onCreate(Bundle saved) {
		super.onCreate(saved);
		setContentView(R.layout.details_activity);

		long id = getIntent().getLongExtra("taskId", -1);
		Log.d(TAG, "_id = " + id);
		if (id > 0) {
			mTask = ((TodoMoreApplication) getApplication()).getTaskDao().findById(id);
		} else {
			mTask = new AndroidTask();
		}

		mMetawidget = (AndroidMetawidget) findViewById(R.id.metawidget);
		mMetawidget.setConfig(R.raw.metawidget);
        mMetawidget.setToInspect(mTask);
        if (id == -1) {
            mMetawidget.setReadOnly(false);
        }

        if (!mMetawidget.isReadOnly()) {
            enableEditing();
        }
        
        enableEditButton = (Button) findViewById(R.id.enableEditButton);
        enableEditButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Log.d(TAG, "onClick");
				enableEditing();
			}
		});
        
        ((Button) findViewById(R.id.cancelDetailsButton)).setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		((Button) findViewById(R.id.deleteButton)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				deleteCurrent();
			}
		});
	}

	private void deleteCurrent() {
		((TodoMoreApplication) getApplication()).getTaskDao().delete((AndroidTask)mTask);
		finish();
	}
	
	private final View.OnClickListener saver = new View.OnClickListener() {
		public void onClick(View v) {
			Toast.makeText(DetailsActivity.this, "Saving...", Toast.LENGTH_SHORT).show();
			
			// Get the fields from the metawidget subfields into mTask:
			mMetawidget.getWidgetProcessor(SimpleBindingProcessor.class).save(mMetawidget);

			// Do the actual save
			((TodoMoreApplication) getApplication()).getTaskDao().update(mTask);
			
			// If no exception thrown...
			finish();
		}
	};

	private void enableEditing() {
		Log.d(TAG, "enableEditing");
		mMetawidget.setReadOnly(false);
		/*
		mMetawidget.setWidgetBuilder(
			new CompositeWidgetBuilder(new CompositeWidgetBuilderConfig<View, AndroidMetawidget>()
				.setWidgetBuilders(
					new OverriddenWidgetBuilder(), 
					new ReadOnlyWidgetBuilder(),
					new TodoAndroidWidgetBuilder(), 
					new AndroidWidgetBuilder()
				)));
		*/
		mMetawidget.buildWidgets();
		View view = mMetawidget.findViewWithTag("name");
		view.setFocusable(true);
		view.setFocusableInTouchMode(true);
		view.requestFocus();
		// Change the "Done" button to "Cancel"
		((Button) findViewById(R.id.cancelDetailsButton)).setText(R.string.cancelEditing);
		// Lose the Edit button
		enableEditButton.setVisibility(View.GONE);
		// Let the Save button appear, and become active
		Button save = (Button) findViewById(R.id.saveButton);
		save.setVisibility(View.VISIBLE);
		save.setOnClickListener(saver);

	}
}

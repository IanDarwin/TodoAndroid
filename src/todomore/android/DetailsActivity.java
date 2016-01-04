package todomore.android;

import org.metawidget.android.widget.AndroidMetawidget;

import com.darwinsys.todo.model.Task;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

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
            mMetawidget.buildWidgets();
            View view = mMetawidget.findViewWithTag("name");
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.requestFocus();
        }
	}
	
	public void enableEditing(View v) {
		Log.d(TAG, "enableEditing");
		mMetawidget.setReadOnly(false);
	}
}

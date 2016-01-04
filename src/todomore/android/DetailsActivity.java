package todomore.android;

import org.metawidget.android.widget.AndroidMetawidget;

import com.darwinsys.todo.model.Task;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class DetailsActivity extends Activity {

	private Task mTask;

	public void onCreate(Bundle saved) {
		super.onCreate(saved);
		setContentView(R.layout.details_activity);

		long id = getIntent().getLongExtra("taskId", -1);

		if (id != -1) {
			mTask = ((TodoMoreApplication) getApplication()).getTaskDao().findById(id);
		} else {
			mTask = new AndroidTask();
		}

		final AndroidMetawidget metawidget = 
			(AndroidMetawidget) findViewById(R.id.metawidget);
        metawidget.setToInspect(mTask);
        if ( id == -1) {
            metawidget.setReadOnly(false);
        }

        if (!metawidget.isReadOnly()) {
            metawidget.buildWidgets();
            View view = metawidget.findViewWithTag("name");
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.requestFocus();
        }
	}
}

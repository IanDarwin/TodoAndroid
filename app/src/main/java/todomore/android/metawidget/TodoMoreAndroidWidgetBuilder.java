package todomore.android.metawidget;

import java.util.Map;

import org.metawidget.android.widget.AndroidMetawidget;
import org.metawidget.widgetbuilder.iface.WidgetBuilder;

import java.time.LocalDate;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class TodoMoreAndroidWidgetBuilder implements WidgetBuilder<View, AndroidMetawidget> {

	private static final String TAG = "TAWB";

	public TodoMoreAndroidWidgetBuilder() {
		Log.d(TAG, "TodoAndroidWidgetBuilder()");
		// empty
	}
	
	@Override
	public View buildWidget(String elementName, 
			Map<String, String> attributes, 
			AndroidMetawidget metaWidget) {
		// Log.d(TAG, String.format("buildWidget(%s, %s, %s)", elementName, attributes, metaWidget));
		Context context = metaWidget.getContext();
		String type = attributes.get("type");
		String readOnlyStr = attributes.get("read-only");
		boolean readOnly = Boolean.parseBoolean(readOnlyStr);
		if (type.equals(LocalDate.class.getName())) {
			return readOnly ? new TextView(context) : new EditText(context);
		}
		return null;
	}

}

package todomore.android.uiconverter;

import org.metawidget.android.widget.widgetprocessor.binding.simple.Converter;

import com.darwinsys.todo.model.Date;

import android.view.View;

/**
 * MetaWidget converter for our local Date class.
 */

public class DateConverter implements Converter<Date> {

	public DateConverter() {
		System.out.println("DateConverter.DateConverter()");
	}

	public Object convertForView(View widget, Date value) {
		System.out.println("DateConverter.convertForView()");
		if (value == null) {
			return null;
		}
		return value.toString();
	}

	public Date convertFromView(View widget, Object value, Class<?> intoClass) {
		System.out.println("DateConverter.convertFromView()");
		if (value instanceof String) {
			if (((String) value).isEmpty()) {
				return null;
			}
			return new Date(value.toString());
		}
		throw new IllegalArgumentException("Can't parse " + value + " of type " + value.getClass().getName());
	}
}

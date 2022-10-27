package todomore.android.metawidget;

import org.metawidget.android.widget.widgetprocessor.binding.simple.Converter;

import android.view.View;

import java.time.LocalDate;

/**
 * MetaWidget converter for our local Date class.
 */
public class DateConverter implements Converter<LocalDate> {

	public DateConverter() {
		System.out.println("DateConverter.DateConverter()");
	}

	/** Convert from a LocalDate to a String for display */
	@Override
	public Object convertForView(View widget, LocalDate value) {
		// System.out.println("DateConverter.convertForView()");
		if (value == null) {
			return null;
		}
		return value.toString();
	}

	/** Convert from a String in the View to a Date object */
	@Override
	public LocalDate convertFromView(View widget, Object value, Class<?> intoClass) {
		if (value instanceof String) {
			if (((String) value).isEmpty()) {
				return null;
			}
			return LocalDate.parse(value.toString());
		}
		throw new IllegalArgumentException("Can't parse " + value + " of type " + value.getClass().getName());
	}
}

package todomore.android;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.darwinsys.todo.model.Date;

import todomore.android.metawidget.DateConverter;


public class DateConverterTest {

	private static final String TWENTY_TWENTY_STRING = "2020-02-20";
	private static final Date TWENTY_TWENTY_DATE = new Date(2020, 02, 20);
	DateConverter conv;
	
	@Before
	public void setUp() throws Exception {
		conv = new DateConverter();
	}

	@Test
	public void testConvertForView() {
		String ret = (String)conv.convertForView(null, TWENTY_TWENTY_DATE);
		assertEquals(TWENTY_TWENTY_STRING, ret);
	}

	@Test
	public void testConvertFromView() {
		Date ret = conv.convertFromView(null, TWENTY_TWENTY_STRING, String.class);
		assertEquals(TWENTY_TWENTY_DATE, ret);
	}

}

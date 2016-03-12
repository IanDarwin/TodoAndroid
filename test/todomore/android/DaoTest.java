package todomore.android;

import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import com.darwinsys.todo.model.Priority;
import com.darwinsys.todo.model.Task;

import android.content.ContentValues;

@RunWith(RobolectricTestRunner.class)
public class DaoTest {
	
	TaskDao mDao;
	
	@Before
	public void setup() {
		mDao = mock(TaskDao.class);
	}

	@Test @Ignore
	public void testFindAll() {
		AndroidTask t = new AndroidTask();
		t.setName("Buy a Tesla on the way home");
		t.setPriority(Priority.High);
		mDao.insert(t);
		
    	List<AndroidTask> todos = mDao.findAll();
    	// Mock DAO doesn't have this much logic yet
    	// assertEquals(1, todos.size());
    	for (Task td : todos) {
    		System.out.println("DAO Found " + td.getName());
    	}
	}
	
	// Can't test w/o Robolectric or Android Unit Test
//	@Test
//	public void testCV() {
//		ContentValues cv = new ContentValues();
//		cv.put("string", "string");
//		cv.put("long", 356L);
//		dump("initial", cv);
//		cv.remove("long");
//		assertNull(cv.getAsLong("long"));
//	}
	
	private void dump(String title, ContentValues cv) {
		System.out.println("---" + title + "---");
		for (String k : cv.keySet()) {
			System.out.println(k + "-->" + cv.getAsString(k));
		}
	}
}

package todomore.android;

import java.util.List;

import com.darwinsys.todo.model.Priority;
import com.darwinsys.todo.model.Task;

public class DaoTest {
	
	TaskDao mDao;

	public void testFindAll() {
		Task t = new Task();
		t.setName("Buy a Tesla on the way home");
		t.setPriority(Priority.High);
		mDao.insert(t);
		
    	List<Task> todos = mDao.findAll();
    	// assertEquals(1, todos.size());
    	for (Task td : todos) {
    		System.out.println("DAO Found " + td.getName());
    	}
	}
}

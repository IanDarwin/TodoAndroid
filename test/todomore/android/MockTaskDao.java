package todomore.android;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

/**
 * In-memory mock dao
 * @author ian
 */
public class MockTaskDao extends TaskDao {

	private List<AndroidTask> tasks = new ArrayList<>();
	
	public MockTaskDao(Context context) {
		super(context);
	}
	
	public MockTaskDao() {
		super(mock(Context.class));
	}
	
	@Override
	public long insert(AndroidTask t) {
		tasks.add(t);
		return tasks.size() -1;
	}

	@Override
	public List<AndroidTask> findAll() {
		return tasks;
	}
}

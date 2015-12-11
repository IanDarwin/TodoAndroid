package todomore.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.List;

import org.junit.Test;

import com.darwinsys.todo.model.Priority;
import com.darwinsys.todo.model.Task;

public class GruntWorkTest {

	@Test
	public void testJsonToTaskList() {
		String input = 
				"[{\"id\":102,\"priority\":\"High\",\"name\":\"TEST 2\","
				+ "\"creationDate\":{\"year\":2015,\"month\":11,\"day\":1},"
				+ "\"project\":null,\"context\":{\"id\":100,\"name\":\"Life\"},"
				+ "\"dueDate\":null,\"status\":\"ACTIVE\",\"completedDate\":null,"
				+ "\"modified\":1448292432791,\"description\":\"None\","
				+ "\"complete\":false},"
				// Start of second Task
				+ "{\"id\":103,\"priority\":\"Low\",\"name\":\"Low prio item\","
				+ "\"creationDate\":{\"year\":2015,\"month\":10,\"day\":23},"
				+ "\"project\":null,\"context\":{\"id\":80,\"name\":\"Home\"},"
				+ "\"dueDate\":null,\"status\":\"NEW\","
				+ "\"completedDate\":null,\"modified\":1448312330383,"
				+ "\"description\":\"\",\"complete\":false}]";

		List<Task> actual = GruntWork.jsonStringToListTask(input);
		assertEquals(2, actual.size());
		assertEquals("TEST 2", actual.get(0).getName());
		assertSame(Priority.High, actual.get(0).getPriority());
		assertEquals(1448312330383L, actual.get(1).getModified());
	}
}


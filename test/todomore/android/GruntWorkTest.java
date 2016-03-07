package todomore.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.List;

import org.junit.Test;

import com.darwinsys.todo.model.Priority;

public class GruntWorkTest {

	@Test
	public void testJsonToTaskList() {
		String input = 
				"[{\"id\":94,\"priority\":\"High\",\"name\":\"TEST 2\","
				+ "\"creationDate\":\"2015-11-18\",\"project\":null,\"context\":null,"
				+ "\"dueDate\":\"2015-11-18\",\"status\":\"NEW\",\"completedDate\":null,\"modified\":12345678,"
				+ "\"description\":null,\"complete\":false},"
				// Start of second Task
				+ "{\"id\":51,\"priority\":\"Medium\",\"name\":\"Phillishave 5821 cutters and plastic cap?\","
				+ "\"creationDate\":\"2015-11-18\",\"project\":null,\"context\":null,"
				+ "\"dueDate\":\"2015-11-18\",\"status\":\"NEW\",\"completedDate\":null,\"modified\":123,"
				+ "\"description\":null,\"complete\":false},"
				// Start of third Task
				+ "{\"id\":103,\"priority\":\"Low\",\"name\":\"Low prio item\","
				+ "\"creationDate\":\"2015-11-18\","
				+ "\"project\":null,\"context\":{\"id\":80,\"name\":\"Home\"},"
				+ "\"dueDate\":null,\"status\":\"NEW\","
				+ "\"completedDate\":null,\"modified\":1448312330383,"
				+ "\"description\":\"\",\"complete\":false}]";

		List<AndroidTask> actual = GruntWork.jsonStringToListTask(input);
		assertEquals(3, actual.size());
		assertEquals("TEST 2", actual.get(0).getName());
		assertSame(Priority.High, actual.get(0).getPriority());
		assertEquals(123L, actual.get(1).getModified());
	}
}


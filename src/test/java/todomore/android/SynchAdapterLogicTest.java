package todomore.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.darwinsys.todo.model.Task;

import todomore.android.sync.TodoSyncAdapter;

public class SynchAdapterLogicTest {

	// Inputs
	List<Task> local;
	List<Task> remote;
	// Outputs
	List<Task> toSaveRemotely;
	List<Task> toSaveLocally;
	
	// Test objects
	Task localTaskWithLocalId;
	Task localTaskWithBothIds;
	Task aRemoteTask;
	
	long lastSyncTime;

	// Configure the List<Task>s
	@Before
	public void configureLists() {
		local = new ArrayList<Task>();
		remote  = new ArrayList<Task>();
		// Outputs
		toSaveRemotely  = new ArrayList<Task>();
		toSaveLocally  = new ArrayList<Task>();
		
		lastSyncTime = System.currentTimeMillis();
		// Let the clock tick, so mtimes are after fake synch time used in tests
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// Can't happen
		}
		
		// Test objects
		localTaskWithBothIds = new Task("localTaskWithBothIds", null, "Writing");
		localTaskWithBothIds.setServerId(1234);	// remote id
		localTaskWithBothIds.setDeviceId(1L);	// Local Id
		
		localTaskWithLocalId = new Task("localTaskWithLocalId", "Reno", "Home");
		localTaskWithLocalId.setDeviceId(2L);	// Local Id
		
		aRemoteTask = new Task("remoteTask", "Upgrades", "Work");
		aRemoteTask.setServerId(54321);
	}

	@Test
	public void testSendLocalWithLowModTime() {

		local.add(localTaskWithBothIds);
		local.add(localTaskWithLocalId);
		
		// Call onSynchronize core
		TodoSyncAdapter.algorithm(local, remote, lastSyncTime, toSaveLocally, toSaveRemotely);

		// Assert calls based on lists
		assertTrue(toSaveRemotely.contains(localTaskWithLocalId));
		// Initially we will upload this task as its mtime is zero
		assertTrue(toSaveRemotely.contains(localTaskWithBothIds));
		// So there should be 2
		assertEquals("Upload both", 2, toSaveRemotely.size());
	}
	
	@Test
	public void testSendLocalWithHighModTime() {
		// Set this one's mod time past lastsynchtime so it should be sent
		localTaskWithBothIds.setModified(System.currentTimeMillis() + 1000);
		local.add(localTaskWithBothIds);
		local.add(localTaskWithLocalId);
		
		// Call onSynchronize core
		TodoSyncAdapter.algorithm(local, remote, lastSyncTime, toSaveLocally, toSaveRemotely);

		// Assert calls based on lists
		assertTrue(toSaveRemotely.contains(localTaskWithLocalId));
		// Should upload this task as its mtime is high
		assertTrue(toSaveRemotely.contains(localTaskWithBothIds));
		assertEquals("Size", 2, toSaveRemotely.size());
	}
	
	@Test
	public void testSaveNewRemoteItem() {
		remote.add(aRemoteTask);
		aRemoteTask.setModified(lastSyncTime - 1000);
		TodoSyncAdapter.algorithm(local, remote, lastSyncTime, toSaveLocally, toSaveRemotely);
		assertTrue(toSaveLocally.contains(aRemoteTask));
		assertTrue(!toSaveRemotely.contains(aRemoteTask));
	}

	/**
	 * Test a remote object that's not yet in our database;
	 * it will have a serverId and a deviceID, be in the local
	 * database (so we don't try to delete it), and an
	 * mtime > lastSyncTime so it should be saved locally.
	 */
	@Test
	public void testSaveModifiedRemoteItem() {
		aRemoteTask.setDeviceId(222L); // So it won't be saved for not having a local ID
		aRemoteTask.setModified(System.currentTimeMillis() + 1000); // Should be saved for mtime
		remote.add(aRemoteTask);
		local.add(aRemoteTask);
		TodoSyncAdapter.algorithm(local, remote, lastSyncTime, toSaveLocally, toSaveRemotely);
		assertTrue(toSaveLocally.contains(aRemoteTask));
		assertTrue(!toSaveRemotely.contains(aRemoteTask));
	}
}


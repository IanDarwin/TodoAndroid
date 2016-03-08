package todomore.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import todomore.android.sync.TodoSyncAdapter;

public class SynchAdapterLogicTest {

	// Inputs
	List<AndroidTask> local;
	List<AndroidTask> remote;
	// Outputs
	List<AndroidTask> toSaveRemotely;
	List<AndroidTask> toSaveLocally;
	
	// Test objects
	AndroidTask localTaskWithLocalId;
	AndroidTask localTaskWithBothIds;
	AndroidTask aRemoteTask;
	
	long lastSyncTime;

	// Configure the List<Task>s
	@Before
	public void configureLists() {
		local = new ArrayList<>();
		remote  = new ArrayList<>();
		// Outputs
		toSaveRemotely  = new ArrayList<>();
		toSaveLocally  = new ArrayList<>();
		
		lastSyncTime = System.currentTimeMillis();
		// Let the clock tick so mtimes are after fake synch time used in tests
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// Can't happen
		}
		
		// Test objects
		localTaskWithBothIds = new AndroidTask("localTaskWithBothIds", null, "Writing");
		localTaskWithBothIds.setId(1234);	// remote id
		localTaskWithBothIds.set_Id(1);	// Local Id
		
		localTaskWithLocalId = new AndroidTask("localTaskWithLocalId", "Reno", "Home");
		localTaskWithLocalId.set_Id(2);	// Local Id
		
		aRemoteTask = new AndroidTask("remoteTask", "Upgrades", "Work");
		aRemoteTask.setId(54321);
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
		TodoSyncAdapter.algorithm(local, remote, lastSyncTime, toSaveLocally, toSaveRemotely);
		assertTrue(toSaveLocally.contains(aRemoteTask));
	}

	@Test
	public void testSaveModifiedRemoteItem() {
		aRemoteTask.set_Id(222); // So it won't be saved for not having a local ID
		aRemoteTask.setModified(System.currentTimeMillis() + 1000); // Should be saved for mtime
		remote.add(aRemoteTask);
		TodoSyncAdapter.algorithm(local, remote, lastSyncTime, toSaveLocally, toSaveRemotely);
		assertTrue(toSaveLocally.contains(aRemoteTask));
	}
}


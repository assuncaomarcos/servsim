package me.marcosassuncao.servsim.scheduler;

import java.util.ArrayList;
import java.util.UUID;

import org.junit.Test;

import me.marcosassuncao.servsim.SimEvent;
import me.marcosassuncao.servsim.Simulation;
import me.marcosassuncao.servsim.event.EventListener;
import me.marcosassuncao.servsim.job.Job;
import me.marcosassuncao.servsim.job.WorkUnit;
import me.marcosassuncao.servsim.job.WorkUnitEvent;
import me.marcosassuncao.servsim.server.JobRequest;
import me.marcosassuncao.servsim.server.Server;
import me.marcosassuncao.servsim.server.TestUser;
import static org.junit.Assert.assertEquals;

public class SchedulerListenerTest implements EventListener<WorkUnitEvent>  {
	private Server srv;
	private TestUser user;
	private int capacity = 10;
	private int jobDuration = 100;
	private int numJobs = 10;
	private int inExecEvents = 0;
	private int completeEvents = 0;
	
	@Override
	public boolean test(WorkUnitEvent t) {
		return true;
	}

	@Override
	public void event(WorkUnitEvent event) {
		if (event.subject().getStatus() == WorkUnit.Status.IN_EXECUTION) {
			inExecEvents++;
		} else if (event.subject().getStatus() == WorkUnit.Status.COMPLETE) {
			completeEvents++;
		}
	}

	@Test
	public void testScheduling() {
		srv = Server.builder()
				.setName("Server-" + UUID.randomUUID())
				.setWorkUnitEventListener(this)
				.setCapacity(capacity).build();
		
		ArrayList<JobRequest> requests = new ArrayList<>();
		for (int i = 0; i < numJobs; i++) {
			// 100 seconds duration, use half capacity
			Job j = new Job(jobDuration, 5);
			requests.add(new JobRequest(j, SimEvent.SEND_NOW));
		}
		
		user = new TestUser(srv.getId(), requests);

		// Simulation trigger class
		Simulation sim = new Simulation() {
			@Override
			public void onConfigure() {
				super.registerEntity(srv);
				super.registerEntity(user);
			}
		};
		sim.run();
		
		assertEquals(numJobs, inExecEvents);
		assertEquals(numJobs, completeEvents);
	}
}
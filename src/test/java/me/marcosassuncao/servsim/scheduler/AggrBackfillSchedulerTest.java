package me.marcosassuncao.servsim.scheduler;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.UUID;

import me.marcosassuncao.servsim.SimEvent;
import me.marcosassuncao.servsim.Simulation;
import me.marcosassuncao.servsim.job.Job;
import me.marcosassuncao.servsim.server.JobRequest;
import me.marcosassuncao.servsim.server.Server;
import me.marcosassuncao.servsim.server.TestUser;

import org.junit.Test;

public class AggrBackfillSchedulerTest {
	private Server srv;
	private TestUser user;
	private int capacity = 10;
	private int jobDuration = 100;
	private int numJobs = 10;

	@Test
	public void testScheduling() {
		srv = Server.builder()
				.setName("Server-" + UUID.randomUUID())
				.setScheduler(new AggrBackfillScheduler())
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
		
		assertEquals(user.receivedJobs().size(), numJobs);
	}
}
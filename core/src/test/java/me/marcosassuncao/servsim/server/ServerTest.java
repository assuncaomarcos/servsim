package me.marcosassuncao.servsim.server;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import me.marcosassuncao.servsim.SimEvent;
import me.marcosassuncao.servsim.Simulation;
import me.marcosassuncao.servsim.job.Job;
import me.marcosassuncao.servsim.scheduler.DefaultScheduler;

import org.junit.Test;

public class ServerTest {
	Server srv;
	TestUser user;
	String srvName = "Server 1";
	int capacity = 10;
	int numJobs = 10;

	@Test
	public void testBuilder() {
		srv = Server.builder()
				.setName(srvName)
				.setCapacity(capacity).build();
		
		assertEquals(srv.getServerAttributes().getResourcePool().getCapacity(), capacity);
		assertEquals(srv.getSchedulingPolicy().getClass().getSimpleName(), DefaultScheduler.class.getSimpleName());
		assertEquals(srv.getServerAttributes().getResourceAvailability().getAvailability(), 1f, 0.00001);
		assertEquals(srv.getName(), srvName);
	}
	
	@Test
	public void runOneServerSimulation() {
		srv = Server.builder()
				.setName(srvName)
				.setCapacity(capacity).build();
		
		ArrayList<JobRequest> requests = new ArrayList<>();
		for (int i = 0; i < this.numJobs; i++) {
			// 10 seconds, use whole capacity
			Job j = new Job(10, capacity);
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
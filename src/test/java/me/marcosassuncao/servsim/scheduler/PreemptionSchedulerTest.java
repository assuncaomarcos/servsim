package me.marcosassuncao.servsim.scheduler;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import me.marcosassuncao.servsim.SimEvent;
import me.marcosassuncao.servsim.Simulation;
import me.marcosassuncao.servsim.job.Job;
import me.marcosassuncao.servsim.server.JobRequest;
import me.marcosassuncao.servsim.server.Server;
import me.marcosassuncao.servsim.server.TestUser;

import org.junit.Test;

public class PreemptionSchedulerTest {
	private Server srv;
	private TestUser user;
	private final int capacity = 1;
	private final int jobDuration = 100;

	@Test
	public void testPreemption() {
		PreemptionScheduler sched = new PreemptionScheduler();
		sched.setSortingComparator(SortAlgorithm.HPF.comparator());
		srv = Server.builder()
				.setName("Server-" + UUID.randomUUID())
				.setScheduler(sched)
				.setCapacity(capacity).build();

		ArrayList<JobRequest> requests = new ArrayList<>();
		Job j = new Job(jobDuration);
		j.setPriority(1);
		requests.add(new JobRequest(j, SimEvent.SEND_NOW));

		j = new Job(jobDuration);
		j.setPriority(0);
		requests.add(new JobRequest(j, 50));

		j = new Job(jobDuration);
		j.setPriority(0);
		requests.add(new JobRequest(j, 170));

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

		assertTrue(requests.get(0).job().getFinishTime() > requests.get(1).job().getFinishTime());
		assertTrue(requests.get(0).job().getFinishTime() > requests.get(2).job().getFinishTime());

		assertTrue(requests.get(0).job().getStartTime() < requests.get(1).job().getStartTime());
		assertTrue(requests.get(0).job().getStartTime() < requests.get(2).job().getStartTime());
	}

	@Test
	public void testEDFSorting() {
		PreemptionScheduler sched = new PreemptionScheduler();
		sched.setSortingComparator(SortAlgorithm.EDF.comparator());
		srv = Server.builder()
				.setName("Server-" + UUID.randomUUID())
				.setScheduler(sched)
				.setCapacity(capacity).build();

		int numJobs = 10;
		ArrayList<JobRequest> requests = new ArrayList<>();
		for (int i = 0; i < numJobs; i++) {
			Job j = new Job(jobDuration, jobDuration * (numJobs+2), 0);
			requests.add(new JobRequest(j, SimEvent.SEND_NOW));
		}

		Collections.shuffle(requests);
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

		for (int i = 0; i < numJobs-1; i++) {
			assertTrue(requests.get(i).job().getDeadlineDuration() <= requests.get(i+1).job().getDeadlineDuration());
		}
	}
}

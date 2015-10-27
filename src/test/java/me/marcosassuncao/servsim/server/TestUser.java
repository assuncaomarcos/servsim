package me.marcosassuncao.servsim.server;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import me.marcosassuncao.servsim.job.Job;
import me.marcosassuncao.servsim.job.Reservation;
import me.marcosassuncao.servsim.server.ReservationServerUser;

public class TestUser extends ReservationServerUser {
	private int serverId;
	private List<JobRequest> requests;
	private HashMap<Integer,Job> receivedJobs = new HashMap<>();
	
	public TestUser(int serverId, List<JobRequest> requests) {
		super("User-" + UUID.randomUUID());
		this.serverId = serverId;
		this.requests = requests;
	}
	
	@Override
	public void onStart() {
		for (JobRequest r : requests) {
			super.submitJob(serverId, r.delay(), r.job());
		}
	}
	
	public HashMap<Integer,Job> receivedJobs() {
		return this.receivedJobs;
	}

	@Override
	public void onReservationResponse(int src, Reservation res) {
	
	}

	@Override
	public void onReservationComplete(int src, Reservation res) {
	
	}

	@Override
	public void onJobReceived(int src, Job job) {
		receivedJobs.put(job.getId(), job);
	}
}
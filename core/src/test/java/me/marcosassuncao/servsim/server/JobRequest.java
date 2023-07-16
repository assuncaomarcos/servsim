package me.marcosassuncao.servsim.server;

import me.marcosassuncao.servsim.job.Job;

public class JobRequest {
	private Job job;
	private long delay;
	
	public JobRequest(Job j, long delay) {
		this.job = j;
		this.delay = delay;
	}
	
	public Job job() {
		return job;
	}
	
	public long delay() {
		return delay;
	}
}

package me.marcosassuncao.servsim.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.UUID;

import me.marcosassuncao.servsim.job.Job;
import me.marcosassuncao.servsim.job.WorkUnit;
import me.marcosassuncao.servsim.profile.RangeList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class implements a simple scheduler where the waiting queue 
 * can be ordered in different ways. If no sorting comparator is provided,
 * the scheduler will present a FIFO behaviour.
 * 
 * @see SortAlgorithmMap
 * 
 * @author Marcos Dias de Assuncao
 */

public class DefaultScheduler extends AbstractScheduler {
	private static final Logger log = LogManager.getLogger(DefaultScheduler.class.getName());
	protected ArrayList<Job> waitingQueue = new ArrayList<>();
	protected ArrayList<Job> runningQueue = new ArrayList<>();
	private Comparator<Job> comparator;
	
	// Filter used to remove events created by preempted jobs
	private FilterJobCompletionEvents filter = new FilterJobCompletionEvents();
	
	/**
	 * Creates a new scheduler instance
	 */
	public DefaultScheduler() {
		super(DefaultScheduler.class.getSimpleName() + "-" + UUID.randomUUID());
	}
	
	/**
	 * Creates simple scheduler.
	 * @throws IllegalArgumentException 
	 */
	public DefaultScheduler(String name) throws IllegalArgumentException {
		super(name);
	}
	
	/**
	 * Sets the comparator used to sort the waiting queue
	 * @param the comparator used to sort the waiting queue
	 */
	public void setSortingComparator(Comparator<Job> comp) {
		this.comparator = comp;
	}

	@Override
	public void doJobCancel(int id) {  
		log.trace("Cancelling job #" + id);
		
		Job cjob = null;
		Iterator<Job> it = this.runningQueue.iterator();
		while (it.hasNext()) {
			Job j = it.next();
			if (j.getId() == id) {
				cjob = j;
				// return time slot to the pool
				long startTime = super.currentTime();
				long jobStartTime = Math.max(0, j.getStartTime());
				long finishTime = startTime + j.getDuration() - (startTime - jobStartTime);
				RangeList res = j.getResourceRanges();
				super.serverAttributes().getResourcePool().releaseResources(startTime, finishTime, res);
				it.remove();
				
				// remove completion events from the simulation queue
				this.filter.setJobId(cjob.getId());
				super.getSimulation().cancelFutureEvents(filter);
				break;
			}
		}
		
		if (cjob != null) {
			// If job was running, cancelling it freed a slot,
			// hence start waiting jobs that can be started
			startQueuedJobs();
		} else {
			it = this.waitingQueue.iterator();
			while (it.hasNext()) {
				Job j = it.next();
				if (j.getId() == id) {
					cjob = j;
					it.remove();
					break;
				}
			}	
		}
		
		super.setJobStatus(cjob, WorkUnit.Status.CANCELLED);
		sendJobToOwner(cjob);
	}
	
	/**
	 * Calling this method forces the scheduler to make an
	 * attempt to start jobs waiting in the queue
	 */
	protected void startQueuedJobs() {
		Collections.sort(waitingQueue, comparator);
		boolean success = true;
		Iterator<Job> it = this.waitingQueue.iterator();
		while (it.hasNext() && success) {
			Job tk = it.next();
			success = startJob(tk);
			if (success) {
				this.runningQueue.add(tk);
				it.remove();
			}
		}
	}

	@Override
	public void doJobCompletion(Job job) {		
		log.trace("Completing job #" + job.getId() + 
					  " at " + super.currentTime());

		setJobStatus(job, WorkUnit.Status.COMPLETE);
		this.runningQueue.remove(job);
		
		if (log.isTraceEnabled()) {
			log.trace("Completed job: \n" + job);
		}
		
		startQueuedJobs();
		sendJobToOwner(job);
	}

	@Override
	public void doJobProcessing(Job job) {
		if (startJob(job)) {
			this.runningQueue.add(job);
		} else {
			if (log.isTraceEnabled()) {

				log.trace("Adding job #" + job.getId() + 
						" to waiting queue at time " + super.currentTime());
			}
			
			this.waitingQueue.add(job);
			setJobStatus(job, WorkUnit.Status.WAITING);
			
			if (log.isTraceEnabled()) {
				log.trace("There are " + waitingQueue.size() + 
						" jobs in the waiting queue.");
			
				String msg = "";
				for (Job tkr : runningQueue) {
					msg += "Job #" + tkr.getId() + " completes at " + 
								(tkr.getStartTime() + tkr.getDuration()) + "\n";
				}
				
				log.trace("Current status of the running queue: \n" + msg);
			}
		}
	}

	@Override
	public void onStart() {
	}

	@Override
	public void onShutdown() {
	}
}

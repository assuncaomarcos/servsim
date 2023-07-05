package me.marcosassuncao.servsim.scheduler;

import me.marcosassuncao.servsim.job.Job;
import me.marcosassuncao.servsim.job.WorkUnit;
import me.marcosassuncao.servsim.profile.RangeList;
import me.marcosassuncao.servsim.server.ResourcePool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Abstract class with helper methods common across
 * all backfilling policies.
 * 
 * @author Marcos Dias de Assuncao
 */
public abstract class BackfillingScheduler extends AbstractScheduler {
	private static final Logger log = LogManager.getLogger(BackfillingScheduler.class.getName());
	/**
	 * The queue of jobs waiting for processing.
	 */
	protected ArrayList<Job> waitingQueue = new ArrayList<>();
	/**
	 * The queue of running jobs.
	 */
	protected ArrayList<Job> runningQueue = new ArrayList<>();
	private FilterJobEventsByIDs filterJobEvents = new FilterJobEventsByIDs();
	private Comparator<Job> comparator;
	
	/**
	 * Creates a new scheduler instance
	 */
	public BackfillingScheduler() {
		super(BackfillingScheduler.class.getSimpleName() + "-" + UUID.randomUUID());
	}

	/**
	 * Creates a new backfilling policy
	 * @param name the name of the simulation entity
	 * @throws IllegalArgumentException if name is <code>null</code>
	 */
	public BackfillingScheduler(String name) throws IllegalArgumentException {
		super(name);
	}

	/**
	 * Sets the comparator used to sort the waiting queue in case 
	 * a running job is cancelled. The new evaluation to check what jobs
	 * can start execution and how the remaining jobs are place in
	 * the queue depends on the sorting criteria specified by the
	 * given comparator
	 * @param comp the comparator used to sort the waiting queue
	 */
	public void setSortingComparator(Comparator<Job> comp) {
		this.comparator = comp;
	}
	
	@Override
	public void doJobProcessing(Job job) {
		if (startJob(job)) {
			this.runningQueue.add(job);
		} else {
			enqueueJob(job);
			this.waitingQueue.add(job);
		}
	}
	
	/**
	 * Method called to add a job to the waiting list
	 * in case it cannot be started.
	 * @param j the job to be added
	 */
	protected abstract void enqueueJob(Job j);
	
	/**
	 * Cancel all future simulation events related to a set of jobs
	 * @param jobIds the Ids of the jobs
	 */
	protected void cancelJobEvents(Collection<Integer> jobIds) {
		// cancel future events related to the affected jobs
		this.filterJobEvents.setJobIDs(jobIds);
		super.getSimulation().cancelFutureEvents(this.filterJobEvents);
	}

	/**
	 * Helper method to reschedule waiting jobs
	 */
	protected void rescheduleJobs() {
		Collections.sort(waitingQueue, comparator);
		boolean success = true;
		Iterator<Job> it = this.waitingQueue.iterator();
		while (it.hasNext() && success) {
			Job j = it.next();
			if (j.hasReserved()) {
				continue;
			}
			
			success = super.startJob(j);
			if (success) {
				this.runningQueue.add(j);
				it.remove();
			} else {
				enqueueJob(j);
			}
		}
	}
	
	@Override
	public void doJobCompletion(Job job) {		
		log.trace("Completing job #" + job.getId() + 
					  " at " + super.currentTime());

		super.setJobStatus(job, WorkUnit.Status.COMPLETE);
		this.runningQueue.remove(job);
		
		if (log.isTraceEnabled()) {
			log.trace("Completed job: \n" + job);
		}
		sendJobToOwner(job);
	}
	
    /**
     * This method iterates the waiting jobs list and for each job, 
     * it returns the allocated time slot and resources to the pool.
     * @param time consider jobs whose start time is further than time
     * @return a collection with the IDs of the affected jobs
     */
    protected Collection<Integer> compressSchedule(long time) {
    	Collection<Integer> jobIds = new LinkedList<Integer>();
    	// jobs with reservation cannot be moved
    	ResourcePool rlist = super.serverAttributes().getResourcePool();
    	Iterator<Job> it = this.waitingQueue.iterator();
        while(it.hasNext()) {
        	Job j = it.next();
        	
        	// Skip as it cannot get better than this.
        	if(j.getStartTime() <= time || j.hasReserved()) {
        		continue;
        	}

			long startTime = Math.max(0, j.getStartTime());
			long finishTime = j.getStartTime() + j.getDuration();
			RangeList res = j.getResourceRanges();
			rlist.releaseResources(startTime, finishTime, res);
			jobIds.add(j.getId());
        }
        
        return jobIds;
    }
}

package me.marcosassuncao.servsim.scheduler;

import me.marcosassuncao.servsim.SimEvent;
import me.marcosassuncao.servsim.job.Job;
import me.marcosassuncao.servsim.job.WorkUnit;
import me.marcosassuncao.servsim.profile.ProfileEntry;
import me.marcosassuncao.servsim.profile.RangeList;
import me.marcosassuncao.servsim.server.ResourcePool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import static me.marcosassuncao.servsim.SimEvent.Type.TASK_START;

/**
 * {@link AggrBackfillScheduler} class is an allocation policy for 
 * {@link me.marcosassuncao.servsim.server.Server} that implements aggressive backfilling.
 * The policy is based on the aggressive backfilling 
 * algorithm described in the following paper: <br>
 * <ul>
 * 		<li>Ahuva W. Mu'alem and Dror G. Feitelson, Utilization, Predictability, 
 * 		Workloads, and User Runtime Estimates in Scheduling the IBM SP2
 * 		with Backfilling. IEEE Transactions on Parallel and Distributed 
 * 		Systems, 12:(6), pp. 529-543, 2001.
 * </ul>
 * 
 * <p> Under aggressive (or EASY) backfilling, a job can jump ahead in the
 * queue and start execution only if it does not delay the execution
 * of the first job in the waiting queue. The first job of the
 * waiting queue is often called the pivot.
 * 
 * @author  Marcos Dias de Assuncao
 */

public class AggrBackfillScheduler extends BackfillingScheduler {
	private static final Logger log = LogManager.getLogger(AggrBackfillScheduler.class.getName());
	private Job pivot;
	
	/**
	 * Creates a new scheduler instance
	 */
	public AggrBackfillScheduler() {
		super(AggrBackfillScheduler.class.getSimpleName() + "-" + UUID.randomUUID());
	}

	/**
	 * Creates a new scheduler instance.
	 * @param name a name for the simulation entity
	 * @throws IllegalArgumentException the name is <code>null</code>
	 */
	public AggrBackfillScheduler(String name) throws IllegalArgumentException {
		super(name);
	}

	@Override
	public void doJobCancel(int id) {  
		Job cjob = null;
		boolean wasPivot = false;
		
		// First check if job is the pivot
		if (this.pivot != null && this.pivot.getId() == id) {
			cjob = this.pivot;
			this.pivot = null;
			wasPivot = true;
		}

		// Check if job is in the running queue
		if (cjob == null) {
			Iterator<Job> it = this.runningQueue.iterator();
			while (it.hasNext()) {
				Job j = it.next();
				if (j.getId() == id) {
					cjob = j;
					it.remove();
					break;
				}
			}
		}
		
		if (cjob !=null) {
			// return time slot to the pool
			long startTime = Math.max(super.currentTime(), cjob.getStartTime());
			long finishTime = startTime + cjob.getDuration() - Math.max(0, startTime - cjob.getStartTime());
			RangeList res = cjob.getResourceRanges();
			super.serverAttributes().getResourcePool().releaseResources(startTime, finishTime, res);
			Collection<Integer> affectedJobIds = compressSchedule(startTime);
			affectedJobIds.add(cjob.getId());
						
			// cancel future events related to the affected jobs
			super.cancelJobEvents(affectedJobIds);
			
			if (wasPivot) {
				super.waitingQueue.remove(cjob);
			}
						
			// reschedule the waiting jobs
			rescheduleJobs();
		}
		
		if (cjob == null) {
			Iterator<Job> it = this.waitingQueue.iterator();
			while (it.hasNext()) {
				Job j = it.next();
				if (j.getId() == id) {
					cjob = j;
					it.remove();
					break;
				}
			}
		}
		
		if (cjob == null) {
			log.error("Job # " + id + " could not be found for cancellation");
		} 
	}

	@Override
	public void doJobCompletion(Job job) {		
		super.doJobCompletion(job);
		rescheduleJobs();
	}
	
	@Override
	public void process(SimEvent ev) {
		if (ev.type() == TASK_START) {
			// If it received a start event, it must be the pivot
			Job j = (Job)ev.content();
			super.setJobStatus(j, WorkUnit.Status.IN_EXECUTION);
			this.waitingQueue.remove(j);
			this.runningQueue.add(j);
			this.pivot = null;
			rescheduleJobs();
		} else {
			super.process(ev);
		}
	}
	
	@Override
	protected void enqueueJob(Job j) {
		if (this.pivot == null) {
			int numRes = j.getNumReqResources();
			ResourcePool resources = super.serverAttributes().getResourcePool();
			ProfileEntry e = resources.findStartTime(numRes, j.getDuration());
			long startTime = e.getTime();
			RangeList selectedRes = e.getAvailRanges().selectResources(numRes);
			super.allocateResourcesToJob(startTime, j, selectedRes);
			j.setStartTime(startTime);
			this.pivot = j;
			
			if (log.isTraceEnabled()) {
				log.trace("Adding job #" + j.getId() + 
						" to waiting queue to start at time " + startTime + 
						" to use resources " + selectedRes + ".");
			}
		}
				
		if (log.isTraceEnabled()) {
			log.trace("There are " + waitingQueue.size() + " jobs in the waiting queue.");
		}
	}
	
	@Override
	public void onStart() {
	}

	@Override
	public void onShutdown() {
	}
}

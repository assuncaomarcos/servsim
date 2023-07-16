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

import static me.marcosassuncao.servsim.SimEvent.Type.TASK_COMPLETE;
import static me.marcosassuncao.servsim.SimEvent.Type.TASK_START;

/**
 * {@link ConsBackfillScheduler} class is an allocation policy for 
 * {@link me.marcosassuncao.servsim.server.Server} that implements conservative backfilling.
 * The policy is based on the conservative backfilling algorithm 
 * described in the following papers:
 * <ul>
 * 		<li> Dror G. Feitelson and Ahuva Mu'alem Weil, Utilization and 
 * 		Predictability in Scheduling the IBM SP2 with Backfilling, in 
 * 		Proceedings of the 12th International Parallel Processing Symposium on 
 * 		International Parallel Processing Symposium (IPPS 1998), pp. 542-546.
 * 
 * 		<li>Ahuva W. Mu'alem and Dror G. Feitelson, Utilization, Predictability, 
 * 		Workloads, and User Runtime Estimates in Scheduling the IBM SP2
 * 		with Backfilling. IEEE Transactions on Parallel and Distributed 
 * 		Systems, 12:(6), pp. 529-543, 2001.
 * </ul>
 * <p> Under conservative backfilling, a job can jump ahead in the queue
 * and start execution only if it does not delay any other job
 * in the waiting queue.
 * 
 * @author  Marcos Dias de Assuncao
 */

public class ConsBackfillScheduler extends BackfillingScheduler {
	private static final Logger log = LogManager.getLogger(ConsBackfillScheduler.class.getName());

	/**
	 * Creates a new scheduler instance
	 */
	public ConsBackfillScheduler() {
		super(ConsBackfillScheduler.class.getSimpleName() + "-" + UUID.randomUUID());
	}

	/**
	 * Creates a new scheduler instance.
	 * @param name a name for the simulation entity
	 * @throws IllegalArgumentException the name is <code>null</code>
	 */
	public ConsBackfillScheduler(String name) throws IllegalArgumentException {
		super(name);
	}

	@Override
	public void doJobCancel(int id) {  
		Job cjob = null;
		Iterator<Job> it = this.runningQueue.iterator();
		while (it.hasNext()) {
			Job j = it.next();
			if (j.getId() == id) {
				cjob = j;
				it.remove();
				break;
			}
		}
		
		if (cjob == null) {
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
		
		if (cjob == null) {
			log.error("Job # " + id + " could not be found for cancellation");
		} else {
			// return time slot to the pool
			long startTime = Math.max(super.currentTime(), cjob.getStartTime());
			long finishTime = startTime + cjob.getDuration() - Math.max(0, startTime - cjob.getStartTime());
			RangeList res = cjob.getResourceRanges();
			super.serverAttributes().getResourcePool().releaseResources(startTime, finishTime, res);
			Collection<Integer> affectedJobIds = compressSchedule(startTime);
			affectedJobIds.add(cjob.getId());
			
			// cancel future events related to the affected jobs
			super.cancelJobEvents(affectedJobIds);
			
			// reschedule the waiting jobs
			rescheduleJobs();
		}
	}
	
	@Override
	public void process(SimEvent ev) {
		if (ev.type() == TASK_START) {
			Job j = (Job)ev.content();
			super.setJobStatus(j, WorkUnit.Status.IN_EXECUTION);
			this.waitingQueue.remove(j);
			this.runningQueue.add(j);
		} else {
			super.process(ev);
		}
	}

	@Override
	protected void enqueueJob(Job j) {
		int numRes = j.getNumReqResources();
		ResourcePool resources = super.serverAttributes().getResourcePool();
		ProfileEntry e = resources.findStartTime(numRes, j.getDuration());
		long startTime = e.getTime();
		RangeList selectedRes = e.getAvailRanges().selectResources(numRes);
		allocateResourcesToJob(startTime, j, selectedRes);
		j.setStartTime(startTime);
		
		if (log.isTraceEnabled()) {
			log.trace("Adding job #" + j.getId() + 
					" to waiting queue to start at time " + startTime + 
					" to use resources " + selectedRes + ".");
		}
		
		if (log.isTraceEnabled()) {
			log.trace("There are " + waitingQueue.size() + 
					" jobs in the waiting queue.");
		}
	}
	
	@Override
	protected void allocateResourcesToJob(long time, Job job, RangeList res) {
		super.allocateResourcesToJob(time, job, res);
		long finishTime = time - super.currentTime() + job.getDuration();
		super.send(super.getId(), finishTime, TASK_COMPLETE, job);
	}
	
	@Override
	public void onStart() {
	}

	@Override
	public void onShutdown() {
	}
}
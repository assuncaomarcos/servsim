package me.marcosassuncao.servsim.scheduler;

import me.marcosassuncao.servsim.job.Job;
import me.marcosassuncao.servsim.job.WorkUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

/**
 * This class implements a simple scheduler where the waiting queue
 * can be ordered in different ways. If no sorting comparator is provided,
 * the scheduler will present a FIFO behaviour.
 *
 * @author Marcos Dias de Assuncao
 */

public class DefaultScheduler extends AbstractScheduler {

	/** Default logger. */
	private static final Logger LOGGER =
			LogManager.getLogger(DefaultScheduler.class.getName());

	/** Queue of jobs waiting for execution. */
	private final JobQueue waitingQueue = new JobQueue();

	/** Queue with jobs in execution. */
	private final JobQueue runningQueue = new JobQueue();


	/**
	 * Creates a new scheduler instance.
	 */
	public DefaultScheduler() {
		super(String.format("%s-%s",
				DefaultScheduler.class.getSimpleName(),
				UUID.randomUUID()));
	}

	/**
	 * Creates a new scheduler instance.
	 * @param name a name for the simulation entity
	 * @throws IllegalArgumentException the name is <code>null</code>
	 */
	public DefaultScheduler(final String name)
			throws IllegalArgumentException {
		super(name);
	}

	@Override
	public void doJobCancel(int id) {
		LOGGER.trace("Cancelling job #" + id);

		Optional<Job> job = runningQueue.removeJob(id);
		if (job.isPresent()) {
			super.cancelJob(job.get());
			// If job was running, cancelling it freed a slot,
			// hence start waiting jobs that can be started
			startQueuedJobs();
		} else {
			job = waitingQueue.removeJob(id);
		}

		if (job.isPresent()) {
			super.setJobStatus(job.get(), WorkUnit.Status.CANCELLED);
			sendJobToOwner(job.get());
		}
	}

	/**
	 * Calling this method forces the scheduler to make an
	 * attempt to start jobs waiting in the queue.
	 */
	protected void startQueuedJobs() {
		boolean success = true;
		Iterator<Job> it = waitingQueue.iterator();
		while (it.hasNext() && success) {
			Job tk = it.next();
			success = startJob(tk);
			if (success) {
				runningQueue.add(tk);
				it.remove();
			}
		}
	}

	@Override
	public void doJobCompletion(Job job) {
		LOGGER.trace("Completing job #" + job.getId() +
					  " at " + super.currentTime());

		setJobStatus(job, WorkUnit.Status.COMPLETE);
		this.runningQueue.remove(job);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Completed job: \n" + job);
		}

		startQueuedJobs();
		sendJobToOwner(job);
	}

	@Override
	public void doJobProcessing(Job job) {
		if (startJob(job)) {
			runningQueue.add(job);
		} else {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Adding job #" + job.getId() +
						" to waiting queue at time " + super.currentTime());
			}

			this.waitingQueue.add(job);
			setJobStatus(job, WorkUnit.Status.WAITING);

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("There are " + waitingQueue.size() +
						" jobs in the waiting queue.");

				StringBuilder msg = new StringBuilder();
				for (Job tkr : runningQueue) {
					msg.append("Job #").append(tkr.getId()).append(" completes at ")
							.append(tkr.getStartTime() + tkr.getDuration()).append("\n");
				}

				LOGGER.trace("Current status of the running queue: \n" + msg);
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

package me.marcosassuncao.servsim.scheduler;

import java.util.LinkedList;

import me.marcosassuncao.servsim.SimEntity;
import me.marcosassuncao.servsim.SimEvent;
import me.marcosassuncao.servsim.event.EventListener;
import me.marcosassuncao.servsim.event.ListenerService;
import me.marcosassuncao.servsim.job.DefaultWorkUnit;
import me.marcosassuncao.servsim.job.Job;
import me.marcosassuncao.servsim.job.WorkUnit;
import me.marcosassuncao.servsim.job.WorkUnitEvent;
import me.marcosassuncao.servsim.profile.ProfileEntry;
import me.marcosassuncao.servsim.profile.RangeList;
import me.marcosassuncao.servsim.server.ResourcePool;
import me.marcosassuncao.servsim.server.ServerAttributes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static me.marcosassuncao.servsim.SimEvent.Type.RESULT_ARRIVE;
import static me.marcosassuncao.servsim.SimEvent.Type.TASK_COMPLETE;
import static me.marcosassuncao.servsim.SimEvent.Type.TASK_START;
import static me.marcosassuncao.servsim.job.WorkUnit.Status.*;
import static me.marcosassuncao.servsim.job.WorkUnitEvent.Type.STATUS_CHANGED;

/**
 * Each server has a scheduling policy that defines how the server resources
 * are allocated to work units. This class implements the basic functionality
 * that a scheduling policy should provide.
 *
 * @author Marcos Dias de Assuncao
 */

public abstract class AbstractScheduler extends SimEntity implements Scheduler,
		ListenerService<WorkUnitEvent, EventListener<WorkUnitEvent>> {

	private static final Logger log = LogManager.getLogger(AbstractScheduler.class.getName());
	/**
	 * The server attributes, such as availability, cluster resources, etc
	 */
	protected ServerAttributes attr;
	/**
	 * The listeners registered to events of this scheduler.
	 */
	protected LinkedList<EventListener<WorkUnitEvent>> listeners;

	/**
	 * Creates a new scheduling policy
	 * @param name the policy's name
	 * @throws IllegalArgumentException if name is <code>null</code>
	 */
	public AbstractScheduler(String name) throws IllegalArgumentException {
		super(name);
	}

	/**
	 * Initialise the scheduling policy.
	 * @param attr the server's attributes
	 */
	public void initialize(ServerAttributes attr) {
		this.attr = attr;
	}

	/**
	 * Gets the server attributes
	 * @return the server attributes
	 */
	public ServerAttributes serverAttributes() {
		return attr;
	}

	@Override
	public void addListener(EventListener<WorkUnitEvent> listener) {
		if (listeners == null) {
			listeners = new LinkedList<>();
		}
		listeners.add(listener);
	}

	@Override
	public void removeListener(EventListener<WorkUnitEvent> listener) {
		if (listeners != null) {
			listeners.remove(listener);
		}
	}

	@Override
	public abstract void onStart();

	@Override
	public abstract void onShutdown();

	@Override
	public void process(SimEvent ev) {
		if (ev.type() == TASK_COMPLETE) {
			try {
				doJobCompletion((Job)ev.content());
			} catch(ClassCastException cce) {
				log.error("Invalid job received for completion.");
			}
		} else {
			log.warn("Unknown payload type: " + ev.content());
		}
	}

	/**
	 * Allocates a resource to a given job
	 * @param time the start time of the allocation
	 * @param job the job to which the resource will be allocated
	 * @param res the resource range list to be allocated.
	 */
	protected void allocateResourcesToJob(long time, Job job, RangeList res) {
		ResourcePool resources = attr.getResourcePool();
		long now = super.currentTime();
		resources.allocateResources(job, res, time);

		// if start time is in the future, then schedule an event to the
		// scheduler itself to signal when the task must be started
		super.send(super.getId(), time - now, TASK_START, job);
		setJobStatus(job, WAITING);
		job.setResourceRanges(res);
	}

	/**
	 * Allocates a resource to a given job
	 * @param job the job to which the resource will be allocated
	 * @param res the resource range list to be allocated.
	 */
	protected void allocateResourcesToJob(Job job, RangeList res) {
		setJobStatus(job, IN_EXECUTION);

		ResourcePool resources = attr.getResourcePool();
		long now = super.currentTime();
		resources.allocateResources(res, now, now + job.getRemainingWork());

		// schedule an event to be handled at the completion of the job
		super.send(super.getId(), job.getRemainingWork(), TASK_COMPLETE, job);
		job.setResourceRanges(res);
	}

	/**
	 * Sends a job back to its owner. That is, schedules an event for the owner
	 * to receive the job and process it.
	 * @param job the job to be returned to the owner
	 */
	protected void sendJobToOwner(Job job) {
		if (job.getOwnerEntityId() == -1) {
			log.trace("Job #" + job.getId() + " does not have an owner.");
		} else {
			super.send(job.getOwnerEntityId(), SimEvent.SEND_NOW, RESULT_ARRIVE, job);
		}
	}

	/**
	 * Tries to start a job on the first available resource and
	 * allocates required resources for it
	 * @param j the job to be started
	 * @return <code>true</code> if the job has been started;
	 * <code>false</code> otherwise
	 */
	protected boolean startJob(Job j) {
		ResourcePool resources = this.attr.getResourcePool();
		long now = super.currentTime();
		ProfileEntry e = resources.checkAvailability(j.getNumReqResources(), now, j.getRemainingWork());

		if (e != null && e.getAvailRanges().getNumItems() >= j.getNumReqResources()) {
			RangeList selected = e.getAvailRanges().selectResources(j.getNumReqResources());
			allocateResourcesToJob(j, selected);

			log.trace("Starting job #" + j.getId() + " at " + super.currentTime());
			return true;
		}
		return false;
	}

	/**
	 * Helper method to fire a job status change
	 * @param u the job whose status changed
	 * @param prevSt the previous status
	 * @param newSt the new status
	 */
	protected void fireStatusChange(DefaultWorkUnit u, WorkUnit.Status prevSt, WorkUnit.Status newSt) {
		if (listeners != null) {
			WorkUnitEvent ev = new WorkUnitEvent(STATUS_CHANGED, u, super.currentTime(), prevSt);
			listeners.forEach(l -> l.event(ev));
		}
	}

	/**
	 * Helper method to set a job status
	 * @param j the job whose status is to be set
	 * @param status the new job status
	 */
	protected void setJobStatus(Job j, WorkUnit.Status status) {
		WorkUnit.Status prevStatus = j.getStatus();
		j.setStatus(status, super.currentTime());
		fireStatusChange(j, prevStatus, status);
	}

	/**
	 * Method to handle the job arrival
	 * @param job the job
	 */
	public abstract void doJobProcessing(Job job);

	/**
	 * Method to handle the completion of a job
	 * @param job the job
	 */
	public abstract void doJobCompletion(Job job);

	/**
	 * Method to handle the cancellation of a job
	 * @param id the job
	 */
	public abstract void doJobCancel(int id);

}

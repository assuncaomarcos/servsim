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
import static me.marcosassuncao.servsim.job.WorkUnit.Status.IN_EXECUTION;
import static me.marcosassuncao.servsim.job.WorkUnit.Status.WAITING;
import static me.marcosassuncao.servsim.job.WorkUnitEvent.Type.STATUS_CHANGED;

/**
 * Each server has a scheduling policy that defines how the server resources
 * are allocated to work units. This class implements the basic functionality
 * that a scheduling policy should provide.
 *
 * @author Marcos Dias de Assuncao
 */

public abstract class AbstractScheduler
        extends SimEntity implements Scheduler,
        ListenerService<WorkUnitEvent,
                EventListener<WorkUnitEvent>> {

    /** Default logger. */
    private static final Logger LOGGER =
            LogManager.getLogger(AbstractScheduler.class.getName());
    /**
     * The server attributes, such as availability, cluster resources, etc.
     */
    private ServerAttributes attr;
    /**
     * The listeners registered to events of this scheduler.
     */
    private LinkedList<EventListener<WorkUnitEvent>> listeners;

    /**
     * Filter used to remove completion events of cancelled jobs.
     */
    private final FilterJobCompletionEvents filter = new FilterJobCompletionEvents();

    /**
     * Creates a new scheduling policy.
     * @param name the policy's name
     * @throws IllegalArgumentException if name is <code>null</code>
     */
    public AbstractScheduler(final String name)
            throws IllegalArgumentException {
        super(name);
    }

    /**
     * Initialise the scheduling policy.
     * @param attr the server's attributes
     */
    public void initialize(final ServerAttributes attr) {
        this.attr = attr;
    }

    /**
     * Gets the server attributes.
     * @return the server attributes
     */
    public ServerAttributes serverAttributes() {
        return attr;
    }

    /**
     * Adds a listener for treating events related to status changes jobs.
     *
     * @param listener listener to add
     */
    @Override
    public void addListener(
            final EventListener<WorkUnitEvent> listener) {
        if (listeners == null) {
            listeners = new LinkedList<>();
        }
        listeners.add(listener);
    }

    /**
     * Removes a previously registered job listener.
     *
     * @param listener listener to remove
     */
    @Override
    public void removeListener(
            final EventListener<WorkUnitEvent> listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void onStart();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void onShutdown();

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final SimEvent ev) {
        if (ev.type() == TASK_COMPLETE) {
            try {
                doJobCompletion((Job) ev.content());
            } catch (ClassCastException cce) {
                LOGGER.error("Invalid job received for completion.");
            }
        } else {
            LOGGER.warn("Unknown payload type: " + ev.content());
        }
    }

    /**
     * Allocates a resource to a given job.
     * @param time the start time of the allocation
     * @param job the job to which the resource will be allocated
     * @param res the resource range list to be allocated.
     */
    protected void allocateResourcesToJob(final long time,
                                          final Job job,
                                          final RangeList res) {
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
     * Allocates a resource to a given job.
     * @param job the job to which the resource will be allocated
     * @param res the resource range list to be allocated.
     */
    protected void allocateResourcesToJob(final Job job,
                                          final RangeList res) {
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
    protected void sendJobToOwner(final Job job) {
        if (job.getOwnerEntityId() == -1) {
            LOGGER.trace("Job #" + job.getId() + " does not have an owner.");
        } else {
            super.send(job.getOwnerEntityId(),
                    SimEvent.SEND_NOW, RESULT_ARRIVE, job);
        }
    }

    /**
     * Tries to start a job on the first available resource and
     * allocates required resources for it.
     * @param j the job to be started
     * @return <code>true</code> if the job has been started;
     * <code>false</code> otherwise
     */
    protected boolean startJob(final Job j) {
        ResourcePool resources = this.attr.getResourcePool();
        long now = super.currentTime();
        ProfileEntry e = resources.checkAvailability(j.getNumReqResources(),
                now, j.getRemainingWork());

        if (e != null
                && e.getAvailRanges().getNumItems() >= j.getNumReqResources()) {
            RangeList selected =
                    e.getAvailRanges().selectResources(j.getNumReqResources());
            allocateResourcesToJob(j, selected);

            LOGGER.trace("Starting job #"
                    + j.getId() + " at " + super.currentTime());
            return true;
        }
        return false;
    }

    /**
     * Handles job cancellation.
     * This method returns the used time slot back to the resource
     * pool and cancels future simulation events related to the job.
     * @param job the job to be cancelled.
     */
    protected void cancelJob(Job job) {
        long startTime = super.currentTime();
        long jobStartTime = Math.max(0, job.getStartTime());
        long finishTime = jobStartTime + job.getRemainingWork();
        RangeList res = job.getResourceRanges();
        this.serverAttributes().getResourcePool()
                .releaseResources(startTime, finishTime, res);

        // Remove completion events from the simulation queue
        this.filter.setJobId(job.getId());
        super.getSimulation().cancelFutureEvents(filter);
    }

    /**
     * Helper method to fire a job status change.
     * @param u the job whose status changed
     * @param prevSt the previous status
     * @param newSt the new status
     */
    protected void fireStatusChange(final DefaultWorkUnit u,
                                    final WorkUnit.Status prevSt,
                                    final WorkUnit.Status newSt) {
        if (listeners != null) {
            WorkUnitEvent ev = new WorkUnitEvent(STATUS_CHANGED, u,
                    super.currentTime(), prevSt);
            listeners.forEach(l -> l.event(ev));
        }
    }

    /**
     * Helper method to set a job status.
     * @param j the job whose status is to be set
     * @param status the new job status
     */
    protected void setJobStatus(final Job j,
                                final WorkUnit.Status status) {
        WorkUnit.Status prevStatus = j.getStatus();
        j.setStatus(status, super.currentTime());
        fireStatusChange(j, prevStatus, status);
    }

    /**
     * Method to handle the job arrival.
     * @param job the job
     */
    public abstract void doJobProcessing(Job job);

    /**
     * Method to handle the completion of a job.
     * @param job the job
     */
    public abstract void doJobCompletion(Job job);

    /**
     * Method to handle the cancellation of a job.
     * @param id the job
     */
    public abstract void doJobCancel(int id);

}

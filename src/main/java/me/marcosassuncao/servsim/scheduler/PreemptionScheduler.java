package me.marcosassuncao.servsim.scheduler;

import me.marcosassuncao.servsim.job.Job;
import me.marcosassuncao.servsim.job.JobActivity;
import me.marcosassuncao.servsim.job.WorkUnit;
import me.marcosassuncao.servsim.profile.ProfileEntry;
import me.marcosassuncao.servsim.profile.RangeList;
import me.marcosassuncao.servsim.server.ResourcePool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.UUID;

/**
 * This class implements a scheduler with preemption and where the
 * waiting queue can be ordered in different ways.
 *
 * @author Marcos Dias de Assuncao
 */

public class PreemptionScheduler extends AbstractScheduler {

    /** Default logger. */
    private static final Logger LOGGER =
            LogManager.getLogger(PreemptionScheduler.class.getName());

    /** Queue of jobs waiting for execution. */
    private final ArrayList<Job> waitingQueue = new ArrayList<>();

    /** Queue with jobs in execution. */
    private final ArrayList<Job> runningQueue = new ArrayList<>();

    /**
     * Comparator used to sort queues and determine.
     * the order of job execution. */
    private Comparator<Job> sortComp;

    /**
     * Filter used to remove events created by preempted jobs.
     */
    private final FilterJobCompletionEvents filter = new FilterJobCompletionEvents();

    /**
     * Creates a new scheduler instance.
     */
    public PreemptionScheduler() {
        super(String.format("%s-%s",
                PreemptionScheduler.class.getSimpleName(),
                UUID.randomUUID()));
    }

    /**
     * Creates a new scheduler instance.
     *
     * @param name a name for the simulation entity
     * @throws IllegalArgumentException the name is <code>null</code>
     */
    public PreemptionScheduler(final String name)
            throws IllegalArgumentException {
        super(name);
    }

    /**
     * Sets the comparator used to sort the waiting queue.
     *
     * @param comp the comparator used to sort the waiting queue
     */
    public void setSortingComparator(final Comparator<Job> comp) {
        this.sortComp = comp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doJobCancel(final int id) {
        LOGGER.trace("Cancelling job #" + id);

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
            startWaitingJobs();
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

    /* Helper method to start waiting and paused jobs */
    private void startWaitingJobs() {
        waitingQueue.sort(sortComp);
        boolean success = true;
        Iterator<Job> it = waitingQueue.iterator();
        while (it.hasNext() && success) {
            Job j2 = it.next();
            success = (j2.getStatus() == WorkUnit.Status.PAUSED) ? resumeJob(j2) : startJob(j2);
            if (success) {
                this.runningQueue.add(j2);
                it.remove();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doJobCompletion(Job job) {
        LOGGER.trace("Completing job #" + job.getId() + " at " + super.currentTime());

        super.setJobStatus(job, WorkUnit.Status.COMPLETE);
        this.runningQueue.remove(job);
        LOGGER.trace("Completed job: \n" + job);

        startWaitingJobs();
        sendJobToOwner(job);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doJobProcessing(Job job) {
        LOGGER.trace("Arrival of job #" + job.getId() + " at " + super.currentTime());
        Job prTk;
        if (startJob(job)) {
            if (!this.runningQueue.add(job)) {
                LOGGER.info("Error adding job #" + job.getId() + " to running queue.");
            }
        } else if ((prTk = findJobToPreempt(job)) != null) {
            LOGGER.trace("Preempting job #" + prTk.getId() +
                    " at time: " + super.currentTime() +
                    " to execute job #" + job.getId());

            if (!this.runningQueue.remove(prTk)) {
                LOGGER.trace("Job #" + prTk.getId() + " not found in running queue.");
            }
            super.setJobStatus(prTk, WorkUnit.Status.PAUSED);

            ResourcePool resources = super.serverAttributes().getResourcePool();
            long now = super.currentTime();

            resources.releaseResources(now,
                    now + prTk.getRemainingWork(),
                    prTk.getResourceRanges());

            // remove completion events from the simulation queue
            this.filter.setJobId(prTk.getId());
            super.getSimulation().cancelFutureEvents(filter);

            LOGGER.trace("Prempted job: " + prTk);

            // enqueues the preempted job
            this.waitingQueue.add(prTk);

            // and starts the new job
            if (startJob(job)) {
                this.runningQueue.add(job);
            } else {
                LOGGER.fatal("Job #" + prTk.getId() + " was preempted, but " +
                        "job #" + job.getId() + " has not started.");
            }
        } else {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Adding job #" + job.getId() +
                        " to waiting queue at time " + super.currentTime());
            }

            this.waitingQueue.add(job);
            super.setJobStatus(job, WorkUnit.Status.WAITING);

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

    /* Tries to preempt a job to start executing tk */
    private Job findJobToPreempt(Job j) {
        // Sorts the running queue using the preemption comparator
        runningQueue.sort(sortComp);
        long now = super.currentTime();

        for (int i = runningQueue.size() - 1; i >= 0; i--) {
            Job prJ = runningQueue.get(i);
            JobActivity act = prJ.getCurrentActivity();

            if (now - act.getStartTime() >= prJ.getRemainingWork()) {
                // job is already complete and the simulation will
                // probably received a completion event shortly
                continue;
            }

            if (this.sortComp.compare(j, prJ) < 0) {
                return prJ;
            }
        }
        return null;
    }

    /* tries to resume a job that has been preempted */
    private boolean resumeJob(Job j) {
        ResourcePool resources = super.serverAttributes().getResourcePool();
        long now = super.currentTime();
        ProfileEntry e = resources.checkAvailability(j.getNumReqResources(),
                now, j.getRemainingWork());

        if (e != null) {
            RangeList selected = e.getAvailRanges().selectResources(j.getNumReqResources());
            super.allocateResourcesToJob(j, selected);
            LOGGER.trace("Resuming job #" + j.getId() + " at " + super.currentTime());
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onShutdown() {
    }
}

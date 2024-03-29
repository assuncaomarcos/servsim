package me.marcosassuncao.servsim.job;

import com.google.common.base.MoreObjects;
import me.marcosassuncao.servsim.profile.RangeList;

import java.util.Iterator;
import java.util.LinkedList;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * This class represents a job or unit of work. If preempted, the job
 * keeps track of all the processing time each resource spent on processing it.
 * The processing times are termed as activities.
 * <p>
 * performed by the resources.
 *
 * @author Marcos Dias de Assuncao
 * @see JobActivity
 */

public class Job extends DefaultWorkUnit implements Iterable<JobActivity> {
    /**
     * List of activities or executions. A Job that is preempted
     * might run multiple times and hence create several activities.
     */
    private final LinkedList<JobActivity> activities = new LinkedList<>();

    /**
     * Amount of remaining work to perform (total duration - time executed).
     */
    private long remainingWork;
    /**
     * Deadline duration associated with this job.
     */
    private long deadlineDuration = TIME_NOT_SET;
    /**
     * Determines whether the job can be preempted.
     */
    private boolean preempt = false;

    /**
     * Id of the user creating the job.
     */
    private int userId = ID_NOT_SET;

    /**
     * A reservation ID, if one has been made for the job.
     */
    private int reservationId = ID_NOT_SET;

    /**
     * The object that returns the preemption overhead for a job.
     */
    private static JobResumeOverhead overheadObj = null;

    /**
     * Creates a new job.
     *
     * @param duration the duration of the job
     */
    public Job(final long duration) {
        super(duration, 0);
        this.remainingWork = duration;
        addFirstActivity();
    }

    /**
     * Creates a new job.
     *
     * @param duration      the duration of the job
     * @param nReqResources number of resources required by the job
     */
    public Job(final long duration, final int nReqResources) {
        super(duration);
        this.remainingWork = duration;
        super.setNumReqResources(nReqResources);
        addFirstActivity();
    }

    /**
     * Creates a new job.
     *
     * @param duration         the duration of the job
     * @param deadlineDuration the duration of the deadline
     *                         associated with the job
     * @param priority         the job's priority
     */
    public Job(final long duration,
               final long deadlineDuration,
               final int priority) {
        super(duration, priority);
        this.deadlineDuration = deadlineDuration;
        this.remainingWork = duration;
        addFirstActivity();
    }

    /* Adds the first activity to the job */
    private void addFirstActivity() {
        this.activities.add(new JobActivity());
    }

    /**
     * Sets the reservation id for this job.
     *
     * @param resId the reservation id for this job
     * @throws IllegalArgumentException if <code>resId</code> &lt;= 0
     */
    public void setReservationId(final int resId) {
        checkArgument(resId >= 0, "Reservation ID must be >= 0");
        this.reservationId = resId;
    }

    /**
     * Gets the reservation id.
     *
     * @return the reservation id
     */
    public int getReservationId() {
        return this.reservationId;
    }

    /**
     * Checks if a reservation for this job has been made.
     *
     * @return <code>true</code> if reservation is has been set;
     * <code>false</code> otherwise.
     */
    public boolean hasReserved() {
        return this.reservationId != ID_NOT_SET;
    }

    /**
     * Sets the object that defines the resume overhead of jobs.
     *
     * @param ovObj the object that defines the resume overhead
     */
    public static void setResumeOverhead(final JobResumeOverhead ovObj) {
        overheadObj = ovObj;
    }

    /**
     * Associates a user ID with this job.
     *
     * @param userId the ID of the user
     * @throws IllegalArgumentException if the ID provided is
     *                                  smaller than <code>-1</code>
     */
    public void setUserId(final int userId) throws IllegalArgumentException {
        checkArgument(userId >= 0, "Illegal user ID.");
        this.userId = userId;
    }

    /**
     * Gets the ID of the user associated with this job.
     *
     * @return the ID of the user
     */
    public int getUserId() {
        return this.userId;
    }

    /**
     * Gets the amount of work (i.e. time duration) left to be done.
     *
     * @return the amount of work to be done
     */
    public long getRemainingWork() {
        return this.remainingWork;
    }

    /**
     * Returns the duration of the SLA associated with the job.
     *
     * @return the duration of the SLA
     */
    public long getDeadlineDuration() {
        return deadlineDuration;
    }

    /**
     * Returns the number of activities associated with this job.
     *
     * @return the number of activities associated with this job
     */
    public int getNumActivities() {
        return this.activities.size();
    }

    /**
     * Checks if a job has met its deadline.
     *
     * @return <code>true</code> if it has; <code>false</code> otherwise.
     */
    public boolean metDeadline() {
        return super.getFinishTime()
                <= super.getSubmitTime() + deadlineDuration;
    }

    /**
     * Sets the deadline duration for this job.
     *
     * @param duration the deadline duration.
     */
    public void setDeadlineDuration(final long duration) {
        this.deadlineDuration = duration;
    }

    /**
     * Used to configure the resource to whom this job has been assigned.
     *
     * @param ranges the range of processor/resource Ids
     * @return <code>true</code> if the id has been set;
     * <code>false</code> otherwise.
     * @throws IllegalStateException if this method is called before setting
     *                               the status of the job to
     *                               {@link WorkUnit.Status#IN_EXECUTION}.
     */
    public boolean setResourceRanges(final RangeList ranges) {
        checkState(activities.size() > 0
                        && !activities.getLast().isFinished(),
                "Job status must be %s before changing resource.",
                Status.IN_EXECUTION);
        return activities.getLast().setResourceRanges(ranges);
    }

    /**
     * Gets the resources that currently processing the job.
     *
     * @return the resource who is currently working on the job;
     * <code>null</code> if there are no activities yet.
     */
    public RangeList getResourceRanges() {
        return activities.size() == 0
                ? null
                : activities.getLast().getResourceRanges();
    }

    // update the activities, stop or start new processing activities
    private void setJobActivities(final long time,
                                  final Status prevStatus,
                                  final Status newStatus) {
        // create activity object and add it to the list
        if (newStatus == Status.IN_EXECUTION
                && (prevStatus == Status.WAITING
                || prevStatus == Status.ENQUEUED
                || prevStatus == Status.PAUSED)) {
            if (!activities.getLast().hasStarted()) {
                activities.getLast().start(time);
            } else {
                JobActivity activity = new JobActivity();
                activity.start(time);
                activities.add(activity);
            }
        } else if ((newStatus == Status.PAUSED
                || newStatus == Status.COMPLETE
                || newStatus == Status.CANCELLED
                || newStatus == Status.FAILED)
                && (prevStatus == Status.IN_EXECUTION
                || prevStatus == Status.PAUSED)) {
            // close outstanding activity
            activities.getLast().finish(time);
        }

        // -------- Sets current work left, resume overhead, etc ---------
        if (prevStatus == Status.IN_EXECUTION && newStatus == Status.PAUSED) {
            JobActivity activ = activities.getLast();
            long diff = activ.getResumeOverhead()
                    - activ.getTimeTakenToPerform();

            // add the difference of the overhead to work left
            if (diff > 0) {
                activ.setResumeOverhead(activ.getResumeOverhead() - diff);
                this.remainingWork -= diff;
            }

            this.remainingWork -= activities.getLast().getTimeTakenToPerform();
        } else if (prevStatus == Status.PAUSED
                && newStatus == Status.IN_EXECUTION) {
            long overhead = overheadObj == null
                    ? 0L
                    : overheadObj.getResumeOverhead(this);
            this.remainingWork += overhead;
            activities.getLast().setResumeOverhead(overhead);
        } else if (newStatus == Status.COMPLETE) {
            this.remainingWork = 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setStatus(final Status status, final long time) {
        Status prStatus = super.getStatus();
        if (super.setStatus(status, time)) {
            // update the list of past processing activities
            setJobActivities(time, prStatus, status);
            return true;
        }
        return false;
    }

    /**
     * Returns the current activity being performed.
     *
     * @return the current activity
     * @throws IllegalStateException if this job has not started to execute
     */
    public JobActivity getCurrentActivity() {
        checkState(activities.size() > 0, "No activity being performed");
        return activities.getLast();
    }

    /**
     * Sets the job's preempt status.
     *
     * @param preempt <code>true</code> if the job can be preempted;
     *                <code>false</code> otherwise.
     */
    public void setPreempt(final boolean preempt) {
        this.preempt = preempt;
    }

    /**
     * Checks whether the job can be preempted.
     *
     * @return <code>true</code> if it is; <code>false</code> otherwise.
     */
    public boolean isPreempt() {
        return this.preempt;
    }

    /**
     * Returns an iterator of resource activities.
     *
     * @return the iterator of resource activities.
     */
    public Iterator<JobActivity> iterator() {
        return new ActivityIterator(activities.iterator());
    }

    /* Just for preventing a user from modifying
     * the list of activities */
    static class ActivityIterator implements Iterator<JobActivity> {

        /**
         * Iterator to the list of activities in the job.
         */
        private final Iterator<JobActivity> it;

        ActivityIterator(final Iterator<JobActivity> it) {
            this.it = it;
        }

        public boolean hasNext() {
            return it.hasNext();
        }

        public JobActivity next() {
            return it.next();
        }

        public void remove() {
            throw new UnsupportedOperationException(
                    "Cannot modify activity list.");
        }
    }

    /**
     * Creates a String representation of the job.
     * @return the String representation.
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", getId())
                .add("submission time", getSubmitTime())
                .add("start time", getStartTime())
                .add("finish time", getFinishTime())
                .add("duration", getDuration())
                .add("priority", getPriority())
                .add("status", getStatus())
                .add("deadline duration", getDeadlineDuration())
                .add("remaining work", getRemainingWork())
                .add("activities", activities)
                .toString();
    }

    /**
     * Job Builder.
     */
    public static final class Builder {
        private long duration = 1;
        private int nResources = 1;
        private int priority = 0;
        private long deadline = TIME_NOT_SET;

        /**
         * Sets the job duration.
         *
         * @param duration the job duration
         * @return the builder reference
         */
        public Builder setDuration(final long duration) {
            this.duration = duration;
            return this;
        }

        /**
         * Sets the number of required resources.
         *
         * @param number the number of required resources
         * @return the builder reference
         */
        public Builder setNumberResources(final int number) {
            this.nResources = number;
            return this;
        }

        /**
         * Sets the job priority.
         *
         * @param priority the job priority
         * @return the builder reference
         */
        public Builder setPriority(final int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Sets the job deadline.
         *
         * @param deadline the job deadline
         * @return the builder reference
         */
        public Builder setDeadline(final int deadline) {
            this.deadline = deadline;
            return this;
        }

        /**
         * Builds the job.
         *
         * @return a job object.
         */
        public Job build() {
            Job j = new Job(duration, deadline, priority);
            j.setNumReqResources(nResources);
            return j;
        }
    }
}

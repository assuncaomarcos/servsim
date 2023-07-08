package me.marcosassuncao.servsim.job;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;

import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * This class represents a simple work unit to be processed by a server entity.
 * It's an implementation of {@link DefaultWorkUnit}
 *
 * @author Marcos Dias de Assuncao
 * @see DefaultWorkUnit
 */

public class DefaultWorkUnit implements WorkUnit, Comparable<DefaultWorkUnit> {
    /**
     * The work unit identifier.
     */
    private final int id;

    /**
     * Counter used for next id.
     */
    private static final AtomicInteger ID_COUNTER = new AtomicInteger(0);

    /**
     * The id of the user entity who created this work unit.
     */
    private int ownerId = ID_NOT_SET;

    /**
     * The time the work unit was submitted.
     */
    private long submitTime = TIME_NOT_SET;

    /**
     * The time the work unit starts.
     */
    private long startTime = TIME_NOT_SET;

    /**
     * The time the work unit finishes.
     */
    private long finishTime = TIME_NOT_SET;

    /**
     * The work unit's duration.
     */
    private final long duration;

    /**
     * The work unit's priority.
     */
    private int priority = 0;

    /**
     * The work unit's current status.
     */
    private Status status = Status.UNKNOWN;

    /**
     * Number of resources required by the work unit.
     */
    private int nReqResources = 1;

    /**
     * Creates a new work unit.
     *
     * @param duration the duration of the work unit
     */
    public DefaultWorkUnit(final long duration) {
        this.id = ID_COUNTER.incrementAndGet();
        this.duration = duration;
    }

    /**
     * Creates a new work unit.
     *
     * @param duration the duration of the work unit
     * @param priority the work unit's priority
     */
    public DefaultWorkUnit(final long duration, final int priority) {
        this(duration);
        this.priority = priority;
    }

    /**
     * Sets the number of required resources.
     *
     * @param num the number of required resources
     * @throws IllegalArgumentException if <code>num</code> is smaller
     *                                  or equals to <code>0</code>.
     */
    public void setNumReqResources(final int num) {
        checkArgument(num > 0, "Number of resources must be greater than 0");
        this.nReqResources = num;
    }

    /**
     * Returns the number of required resources.
     *
     * @return the number of required resources
     */
    public int getNumReqResources() {
        return this.nReqResources;
    }

    /**
     * Sets the work unit's start time.
     *
     * @param time the work unit's start time.
     * @throws IllegalArgumentException if time &lt; 0
     */
    public void setStartTime(final long time) {
        checkArgument(time > 0, "Invalid start time: %s", time);
        this.startTime = time;
    }

    /**
     * Sets the finish time.
     *
     * @param time the time to be used
     * @throws IllegalArgumentException if time &gt; 0
     */
    public void setFinishTime(final long time) {
        checkArgument(time > 0, "Invalid finish time: %s", time);
        this.finishTime = time;
    }

    /**
     * Sets the ID of the owner entity of the work unit.
     *
     * @param ownerId the ID of the owner of the work unit
     * @throws IllegalArgumentException if ownerId &lt; 0
     */
    public void setOwnerEntityId(final int ownerId) {
        checkArgument(ownerId > 0, "Owner id cannot be smaller than 0");
        this.ownerId = ownerId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setStatus(final Status to, final long time) {
        if (!to.getConditions().contains(status)) {
            return false;
        }

        switch (to) {
            case IN_EXECUTION -> {
                if (this.status != Status.PAUSED) {
                    this.startTime = time;
                }
            }
            case COMPLETE, CANCELLED, FAILED -> {
                if (this.status == Status.IN_EXECUTION
                        || this.status == Status.PAUSED) {
                    this.finishTime = time;
                }
            }
            default -> {
            }
        }

        this.status = to;
        return true;
    }

    /**
     * Sets the submission time.
     *
     * @param time the submission time
     * @throws IllegalArgumentException if time &lt; 0
     */
    public void setSubmitTime(final long time) {
        checkArgument(time >= 0, "Invalid submission time: %s", time);
        this.submitTime = time;
        setStatus(Status.ENQUEUED, time);
    }

    /**
     * Sets the unit's priority.
     * The lower the number, the higher the job priority.
     *
     * @param priority the unit's priority
     */
    public void setPriority(final int priority) {
        checkArgument(priority >= 0, "Priority must be >=0");
        this.priority = priority;
    }

    /**
     * Gets the work unit id.
     *
     * @return the work unit id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the id of the entity that dispatched the job.
     *
     * @return the id of the entity or <code>-1</code> if not found
     */
    public int getOwnerEntityId() {
        return this.ownerId;
    }

    /**
     * Gets the status of the work unit.
     *
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Returns the unit's submit time.
     *
     * @return the unit's submit time
     */
    public long getSubmitTime() {
        return submitTime;
    }

    /**
     * Returns the work unit's start time.
     *
     * @return the work unit's start time
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Gets the unit's finish time.
     *
     * @return the unit's finish time
     */
    public long getFinishTime() {
        return finishTime;
    }

    /**
     * Returns the response time of the work unit.
     * That is, <code>finish time - submit time</code>
     *
     * @return the response time or <code>-1</code> if not known yet
     */
    public long getResponseTime() {
        return finishTime == TIME_NOT_SET
                ? TIME_NOT_SET
                : finishTime - submitTime;
    }

    /**
     * Returns the time the request waited in queue before
     * starting processing.
     *
     * @return the wait time or <code>-1</code> if not known yet
     */
    public long getWaitTime() {
        return startTime == TIME_NOT_SET
                ? TIME_NOT_SET
                : startTime - submitTime;
    }

    /**
     * Gets the work unit's duration.
     *
     * @return the work unit's duration
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Gets the unit's priority.
     *
     * @return the unit's priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final DefaultWorkUnit o) {
        return ComparisonChain.start()
                .compare(this.submitTime, o.submitTime)
                .compare(this.id, o.id).result();
    }

    /**
     * Creates a String representation of this work unit.
     * @return the String representation.
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("submission time", getSubmitTime())
                .add("start time", getStartTime())
                .add("finish time", getFinishTime())
                .add("duration", duration)
                .add("priority", priority)
                .add("status", status)
                .toString();
    }
}

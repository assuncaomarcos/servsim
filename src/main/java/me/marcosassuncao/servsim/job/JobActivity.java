package me.marcosassuncao.servsim.job;

import static com.google.common.base.Preconditions.checkState;
import static me.marcosassuncao.servsim.job.WorkUnit.TIME_NOT_SET;
import me.marcosassuncao.servsim.profile.RangeList;

import com.google.common.base.MoreObjects;

/**
 * This class is used to keep track of the work carried out
 * by resources when executing a job that is preempted.
 *
 * @author Marcos Dias de Assuncao
 */

public class JobActivity {
    /**
     * When the activity starts.
     */
    private long startTime = TIME_NOT_SET;
    /**
     * Time the activity ends.
     */
    private long finishTime = TIME_NOT_SET;
    /**
     * The amount of overhead to resume an activity, when paused.
     */
    private long resumeOverhead = 0;

    /**
     * The range of resources the activity uses.
     */
    private RangeList rangeList;

    /**
     * Starts the activity.
     * @param time the time of the start of the activity
     * @return <code>true</code> if the activity has started correctly,
     * <code>false</code> otherwise.
     */
    public boolean start(final long time) {
        if (startTime != TIME_NOT_SET) {
            return false;
        }
        this.startTime = time;
        return true;
    }

    /**
     * Sets the resume overhead.
     * @param time the resume overhead.
     */
    public void setResumeOverhead(final long time) {
        this.resumeOverhead = time;
    }

    /**
     * Gets the resume overhead.
     * @return the resume overhead.
     */
    public long getResumeOverhead() {
        return this.resumeOverhead;
    }

    /**
     * Completes the activity.
     * @param time the time of the end of the activity
     * @return <code>true</code> if the activity has completed correctly,
     * <code>false</code> otherwise.
     */
    public boolean finish(final long time) {
        if (finishTime != TIME_NOT_SET) {
            return false;
        }
        this.finishTime = time;
        return true;
    }

    /**
     * Checks if an activity has already started.
     * @return <code>true</code> if it has; <code>false</code> otherwise.
     */
    public boolean hasStarted() {
        return this.startTime >= 0;
    }

    /**
     * Checks if the activity is finished.
     * @return <code>true</code> if the activity is complete.
     */
    public boolean isFinished() {
        return finishTime >= 0;
    }

    /**
     * Gets the time this activity has taken to be performed.
     * @return the time this activity has taken to be performed.
     * @throws IllegalStateException if this method is invoked before
     * the activity has completed.
     */
    public long getTimeTakenToPerform() {
        checkState(startTime != TIME_NOT_SET && finishTime != TIME_NOT_SET,
                            "Activity has not been performed.");
        return finishTime - startTime;
    }

    /**
     * Returns the start time of this activity.
     * @return the start time of the activity or {@link WorkUnit#TIME_NOT_SET}
     * if it is unknown.
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Returns the finish time of this activity.
     * @return the finish time of the activity or {@link WorkUnit#TIME_NOT_SET}
     * if it is unknown.
     */
    public long getFinishTime() {
        return finishTime;
    }

    /**
     * Sets the ranges of resources that execute the job.
     * @param ranges the ranges of resources that carried out the activity
     * @return <code>true</code> if the ranges have been set correctly,
     * <code>false</code> otherwise.
     */
    public boolean setResourceRanges(final RangeList ranges) {
        if (this.rangeList != null) {
            return false;
        }
        this.rangeList = ranges;
        return true;
    }

    /**
     * Gets the ranges of resources that carried out the activity.
     * @return the ranges of resources that carried out the activity
     */
    public RangeList getResourceRanges() {
        return this.rangeList;
    }

    /**
     * Creates a String representation of this activity.
     * @return the String representation.
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("resources", rangeList == null
                        ? "[]"
                        : rangeList.toString())
                .add("start time", getStartTime())
                .add("finish time", getFinishTime())
                .add("finished", isFinished())
                .toString();
    }
}

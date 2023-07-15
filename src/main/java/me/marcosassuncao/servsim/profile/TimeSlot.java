package me.marcosassuncao.servsim.profile;

import com.google.common.base.MoreObjects;

/**
 * This class defines a time slot. A time slot can be used to represent a
 * free fragment in the scheduling queue. This may be useful for users who
 * want to implement policies that perform best-fit, next-fit or worst-fit
 * time slot selection. For a description on how time slots can be utilised,
 * please read the following paper about the MAUI scheduler:
 * <ul>
 * <li> David B. Jackson, Quinn Snell and Mark J. Clement. Core Algorithms
 * of the Maui Scheduler, Revised Papers from the 7th International
 * Workshop on Job Scheduling Strategies for Parallel Processing
 * (JSSPP '01), Lecture Notes in Computer Science, pp. 87-102, London, UK.
 * </ul>
 *
 * @author  Marcos Dias de Assuncao
 *
 * @see Range
 * @see RangeList
 */
public class TimeSlot {

    /** The slot start time. */
    private long startTime;

    /** The finish time. */
    private long finishTime;

    /** The list of resources available during the time slot. */
    private RangeList ranges;

    /**
     * Default constructor sets.
     * @param startTime the start time of the time slot
     * @param finishTime the finish time of the time slot
     * @param ranges the list of PE ranges available at the slot
     */
    public TimeSlot(final long startTime,
                    final long finishTime,
                    final RangeList ranges) {
        this.startTime = startTime;
        this.finishTime = finishTime;
        this.ranges = ranges;
    }

    /**
     * Sets the start time of the time slot.
     * @param startTime the start time, which must be &gt;= 0
     * @return <code>true</code> if set successfully;
     * <code>false</code> otherwise.
     */
    public boolean setStartTime(final long startTime) {
        if (startTime < 0) {
            return false;
        }

        this.startTime = startTime;
        return true;
    }

    /**
     * Sets the finish time of the time slot.
     * @param finishTime the finish time, which must be &gt;= 0
     * @return <code>true</code> if set successfully;
     * <code>false</code> otherwise.
     */
    public boolean setFinishTime(final long finishTime) {
        if (finishTime < 0) {
            return false;
        }

        this.finishTime = finishTime;
        return true;
    }

    /**
     * Sets the ranges of the time slot.
     * @param ranges the ranges of this time slot, which
     * should not be <code>null</code>
     * @return <code>true</code> if set successfully;
     * <code>false</code> otherwise.
     */
    public boolean setResourceRanges(final RangeList ranges) {
        if (ranges == null) {
            return false;
        }

        this.ranges = ranges;
        return true;
    }

    /**
     * Returns the start time of this time slot.
     * @return the start time of this time slot
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Returns the finish time of this time slot.
     * @return the finish time of this time slot
     */
    public long getFinishTime() {
        return finishTime;
    }

    /**
     * Returns the time duration of this time slot.
     * @return the time duration of this time slot.
     */
    public long getDuration() {
        return finishTime - startTime;
    }

    /**
     * Returns the PE ranges available at the time slot.
     * @return the PE ranges available at the time slot
     */
    public RangeList getResourceRanges() {
        return ranges;
    }

    /**
     * Returns the number of PEs available at the time slot.
     * @return the number of PEs available at the time slot
     */
    public int getNumResources() {
        return ranges == null ? 0 : ranges.getNumItems();
    }

    /**
     * Creates a string representation of the list.
     * @return a string representation
     */
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("startTime", startTime)
                .add("finishTime", finishTime)
                .add("ranges", ranges)
                .toString();
    }
}

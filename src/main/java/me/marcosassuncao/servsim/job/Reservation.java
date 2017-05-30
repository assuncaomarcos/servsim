package me.marcosassuncao.servsim.job;

import me.marcosassuncao.servsim.profile.RangeList;

import com.google.common.base.MoreObjects;

/**
 * {@link Reservation} represents a resource reservation request
 * made by a customer to reserve a given number of resources 
 * from a provider.
 * 
 * @see WorkUnit
 * @see DefaultWorkUnit
 * 
 * @author Marcos Dias de Assuncao
 */

public class Reservation extends DefaultWorkUnit {
	private long reqStartTime = WorkUnit.TIME_NOT_SET;	
	private RangeList rangeList;
	
	/**
	 * Creates a reservation request to start at
	 * <code>startTime</code> and with the given duration.
	 * @param startTime the requested start time for the reservation
	 * @param duration the duration of the reservation
	 * @param numResources number of required resources
	 */
	public Reservation(long startTime, 
			long duration, int numResources) {
		super(duration);
		super.setNumReqResources(numResources);
		this.reqStartTime = startTime;
	}
	
	/**
	 * Creates a reservation request to start at
	 * <code>startTime</code> and with the given duration and priority
	 * @param startTime the requested start time for the reservation
	 * @param duration the duration of the reservation
	 * @param numResources the number of resources to be reserved
	 * @param priority the reservation priority
	 */
	public Reservation(long startTime, 
			long duration, int numResources, int priority) {
		super(duration, priority);
		super.setNumReqResources(numResources);
		this.reqStartTime = startTime;
	}
	
	/**
	 * Returns the start time requested by this reservation
	 * @return the requested start time
	 */
	public long getRequestedStartTime() {
		return reqStartTime;
	}
	
	/**
	 * Sets the ranges of reserved resources 
	 * @param ranges the ranges of resources allocated for the reservation
	 * @return <code>true</code> if the ranges have been set correctly,
	 * <code>false</code> otherwise.
	 */
	public boolean setResourceRanges(RangeList ranges) {
		if (this.rangeList != null) {
			return false;
		}
		this.rangeList = ranges;
		return true;
	}
	
	/**
	 * Gets the ranges of reserved resources 
	 * @return the ranges of reserved resources
	 */
	public RangeList getResourceRanges() {
		return rangeList;
	}
	
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
			    .add("resources", rangeList)
			    .toString();
	}
}
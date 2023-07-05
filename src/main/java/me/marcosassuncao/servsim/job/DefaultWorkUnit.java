package me.marcosassuncao.servsim.job;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * This class represents a simple work unit to be processed by a server entity.
 * It's an implementation of {@link DefaultWorkUnit}
 * 
 * @see DefaultWorkUnit
 * 
 * @author Marcos Dias de Assuncao
 */

public class DefaultWorkUnit implements WorkUnit, Comparable<DefaultWorkUnit> {
	private final int id;
	private static int nextId = 0;
	private int ownerId = ID_NOT_SET;
	private long submitTime = TIME_NOT_SET;
	private long startTime = TIME_NOT_SET;
	private long finishTime = TIME_NOT_SET;
	private final long duration;
	private int priority = 0;
	private Status status = Status.UNKNOWN;
	private int nReqResources = 1;
	
	/**
	 * Creates a new work unit
	 * @param duration the duration of the work unit
	 */
	public DefaultWorkUnit(long duration) {
		this.id = createId();
		this.duration = duration;
	}
	
	/**
	 * Creates a new work unit
	 * @param duration the duration of the work unit
	 * @param priority the work unit's priority
	 */
	public DefaultWorkUnit(long duration, int priority) {
		this(duration);
		this.priority = priority;
	}
	
	/**
	 * Sets the number of required resources
	 * @param num the number of required resources
	 * @throws IllegalArgumentException if <code>num</code> is smaller
	 * or equals to <code>0</code>.
	 */
	public void setNumReqResources(int num) {
		checkArgument(num > 0, "Number of resources must be greater than 0");
		this.nReqResources = num;
 	}
	
	/**
	 * Returns the number of required resources
	 * @return the number of required resources
	 */
	public int getNumReqResources() {
		return this.nReqResources;
	}
	
	/**
	 * Sets the work unit's start time.
	 * @param time the work unit's start time.
	 * @throws IllegalArgumentException if time &lt; 0
	 */
	public void setStartTime(long time) {
		checkArgument(time > 0, "Invalid start time: %s", time);
		this.startTime = time;
	}
	
	/**
	 * Sets the finish time
	 * @param time the time to be used
	 * @throws IllegalArgumentException if time &gt; 0
	 */
	public void setFinishTime(long time) {
		checkArgument(time > 0, "Invalid finish time: %s", time);
		this.finishTime = time;
	}
	
	/**
	 * Sets the ID of the owner entity of the work unit
	 * @param ownerId the ID of the owner of the work unit
	 * @throws IllegalArgumentException if ownerId &lt; 0
	 */
	public void setOwnerEntityId(int ownerId) {
		checkArgument(ownerId > 0, "Owner id cannot be smaller than 0");
		this.ownerId = ownerId;
	}
	
	@Override
	public boolean setStatus(Status to, long time) {
		if (!to.getConditions().contains(status)) {
			return false;
		}
		
		switch (to) {
			case IN_EXECUTION:
				if (to != Status.PAUSED) {
					this.startTime = time;
				}
				break;
				
			case COMPLETE:
			case CANCELLED:
			case FAILED:
				if (this.status == Status.IN_EXECUTION || 
					this.status == Status.PAUSED) {
					this.finishTime = time;
				}
				break;
				
			default: break;
		}
		
		this.status = to;
		return true;
	}
	
	/**
	 * Sets the submission time
	 * @param time the submission time
	 * @throws IllegalArgumentException if time &lt; 0
	 */
	public void setSubmitTime(long time) {
		checkArgument(time >= 0, "Invalid submission time: %s", time);
		this.submitTime = time;
		setStatus(Status.ENQUEUED, time);
	}
		
	/**
	 * Sets the unit's priority. 
	 * The lower the number, the higher the job priority.
	 * @param priority the unit's priority
	 */
	public void setPriority(int priority) {
		checkArgument(priority >= 0, "Priority must be >=0");
		this.priority = priority;
	}

	/**
	 * Gets the work unit id
	 * @return the work unit id
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Returns the id of the entity that dispatched the job
	 * @return the id of the entity or <code>-1</code> if not found
	 */
	public int getOwnerEntityId() {
		return this.ownerId;
	}
	
	/**
	 * Gets the status of the work unit
	 * @return the status
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * Returns the unit's submit time
	 * @return the unit's submit time
	 */
	public long getSubmitTime() {
		return submitTime;
	}
	
	/**
	 * Returns the work unit's start time
	 * @return the work unit's start time
	 */
	public long getStartTime() {
		return startTime;
	}
	
	/**
	 * Gets the unit's finish time
	 * @return the unit's finish time
	 */
	public long getFinishTime() {
		return finishTime;
	}
	
	/**
	 * Returns the response time of the work unit. 
	 * That is, <code>finish time - submit time</code>
	 * @return the response time or <code>-1</code> if not known yet
	 */
	public long getResponseTime() {
		return finishTime == TIME_NOT_SET ? TIME_NOT_SET : finishTime - submitTime;
	}
	
	/**
	 * Returns the time the request waited in queue before
	 * starting processing.
	 * @return the wait time or <code>-1</code> if not known yet
	 */
	public long getWaitTime() {
		return startTime == TIME_NOT_SET ? TIME_NOT_SET : startTime - submitTime;
	}
	
	/**
	 * Gets the work unit's duration
	 * @return the work unit's duration
	 */
	public long getDuration() {
		return duration;
	}

	/**
	 * Gets the unit's priority
	 * @return the unit's priority
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * Compares this object with the specified object for order. 
	 * Returns a negative integer, zero, or a positive integer as this 
	 * object is less than, equal to, or greater than the specified object.<br>
	 * <b>NOTE:</b> By default, submission time and work unit id are used
	 * as criteria for ordering the work units.
	 * @see Comparable#compareTo(Object)
	 */
	public int compareTo(DefaultWorkUnit o) {
		return ComparisonChain.start()
			.compare(this.submitTime, o.submitTime)
			.compare(this.id, o.id).result();
	}
	
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
	
	/* synchronised ID creation */
	private static synchronized int createId() {
		return ++nextId;
	}
}

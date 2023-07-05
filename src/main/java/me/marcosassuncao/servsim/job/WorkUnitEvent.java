package me.marcosassuncao.servsim.job;

import me.marcosassuncao.servsim.event.AbstractEvent;

import com.google.common.base.Optional;

/**
 * An event triggered by a change in a work unit.
 * 
 * @author Marcos Dias de Assuncao
 */

public class WorkUnitEvent extends AbstractEvent<WorkUnitEvent.Type, DefaultWorkUnit> {
	private final Optional<WorkUnit.Status> prevStatus;
	
	/**
	 * Creates a new job event.
	 * @param type the type of event
	 * @param subject the job associated with the event
	 * @param time the time at which the event occurred
	 */
	public WorkUnitEvent(long time, Type type, DefaultWorkUnit subject) {
		this(type, subject, time, null);
	}
	
	/**
	 * Creates a new job event.
	 * @param type the type of event
	 * @param subject the job associated with the event
	 * @param time the time at which the event occurred
	 * @param prevStatus the status of the job prior to the occurrence of the event
	 */
	public WorkUnitEvent(Type type, DefaultWorkUnit subject, long time, 
				WorkUnit.Status prevStatus) {
		super(type, subject, time);
		this.prevStatus = Optional.fromNullable(prevStatus);
	}
	
	/**
	 * Returns the status of the job previous to 
	 * the occurrence of the event
	 * @return the previous job status
	 */
	public Optional<WorkUnit.Status> previousStatus() {
		return prevStatus;
	}
	
	/**
	 * Types of job events
	 */
	public enum Type {
		/** The status of a job has changed */
		STATUS_CHANGED
	}
}
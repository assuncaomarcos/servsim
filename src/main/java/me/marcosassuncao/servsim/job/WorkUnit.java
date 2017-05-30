package me.marcosassuncao.servsim.job;

import java.util.Arrays;
import java.util.List;

/**
 * Interface that exposes features of jobs and tasks.
 * 
 * @author Marcos Dias de Assuncao
 */

public interface WorkUnit {
	public static final long TIME_NOT_SET = -1;
	public static final int ID_NOT_SET = -1;
	
	/**
	 * Returns the id of the owner (or user) of the job
	 * @return the id of the owner or <code>-1</code> if not found
	 */
	int getOwnerEntityId();
	
	/**
	 * Sets the status of this task
	 * @param status the status of this task
	 * @param time simulation time at which the status is to be set
	 * @return <code>true</code> if the status has been set; 
	 * <code>false</code> otherwise.
	 */
	boolean setStatus(WorkUnit.Status status, long time);
	
	/**
	 * Sets the submission time
	 * @param time the submission time
	 * @throws IllegalArgumentException if time &lt; 0
	 */
	public void setSubmitTime(long time);
	
	/**
	 * Sets the unit's priority
	 * @param priority the unit's priority
	 */
	void setPriority(int priority);

	/**
	 * Gets the task id
	 * @return the task id
	 */
	int getId();
	
	/**
	 * Gets the status of the task
	 * @return the status
	 */
	WorkUnit.Status getStatus();

	/**
	 * Returns the unit's submit time
	 * @return the unit's submit time
	 */
	long getSubmitTime();
	
	/**
	 * Returns the task's start time
	 * @return the task's start time
	 */
	long getStartTime();
	
	/**
	 * Gets the unit's finish time
	 * @return the unit's finish time
	 */
	long getFinishTime();
	
	/**
	 * Gets the task's duration
	 * @return the task's duration
	 */
	long getDuration();

	/**
	 * Gets the unit's priority
	 * @return the unit's priority
	 */
	int getPriority();
		
	interface StateTransition<T> {
		List<T> getConditions();
	}
	
	/**
	 * Possible task/job statuses
	 */
	public enum Status implements StateTransition<Status> {
		
		/** The unit's status is unknown (the default when the unit is created) */
		UNKNOWN(Arrays.asList()),
		
		/** The unit arrived at the server, but has not been processed by the scheduler yet */
		ENQUEUED(Arrays.asList(UNKNOWN)),
		
		/** The unit has been submitted and is waiting to be processed by a server */
		WAITING(Arrays.asList(ENQUEUED)),
		
		/** The unit's execution has been paused */
		PAUSED(Arrays.asList()) {
			public List<Status> getConditions() {
				return Arrays.asList(IN_EXECUTION);
			}
		},
		
		/** Unit is in execution */
		IN_EXECUTION(Arrays.asList(ENQUEUED, WAITING, PAUSED)),
		
		/** The processing of the unit has finished */
		COMPLETE(Arrays.asList(IN_EXECUTION, PAUSED)),
		
		/** The processing has been cancelled */
		CANCELLED(Arrays.asList(ENQUEUED, WAITING, IN_EXECUTION, PAUSED)),
		
		/** The unit's execution has failed */
		FAILED(Arrays.asList(ENQUEUED, WAITING, IN_EXECUTION, PAUSED));
		
		private List<Status> conditions;

		Status(List<Status> conditions) {
			this.conditions = conditions;
		}
		
		/**
		 * Returns the list of states from which the current state can move
		 * @return a list of states
		 */
		public List<Status> getConditions() {
			return conditions;
		}
	}
}
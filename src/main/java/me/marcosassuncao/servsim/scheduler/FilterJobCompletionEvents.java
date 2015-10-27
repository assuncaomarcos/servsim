package me.marcosassuncao.servsim.scheduler;

import me.marcosassuncao.servsim.SimEvent;
import me.marcosassuncao.servsim.event.EventFilter;
import me.marcosassuncao.servsim.job.WorkUnit;
import static me.marcosassuncao.servsim.SimEvent.Type.TASK_COMPLETE;

/**
 * Filter used to remove the simulation event 
 * that marks the completion of a job. 
 * 
 * @author Marcos Dias de Assuncao
 */
class FilterJobCompletionEvents implements EventFilter<SimEvent> {
	private int jobId;
	
	/**
	 * Sets the ID of the job to be cancelled
	 * @param jobId the ID of the job to be cancelled
	 */
	public void setJobId(int jobId) {
		this.jobId = jobId;
	}

	@Override
	public boolean test(SimEvent ev) {
		return ev.type() == TASK_COMPLETE && ((WorkUnit)ev.content()).getId() == jobId;
	}
}
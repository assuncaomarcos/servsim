package me.marcosassuncao.servsim.scheduler;

import java.util.Collection;
import java.util.HashSet;

import me.marcosassuncao.servsim.SimEvent;
import me.marcosassuncao.servsim.event.EventFilter;
import me.marcosassuncao.servsim.job.WorkUnit;
import static me.marcosassuncao.servsim.SimEvent.Type.TASK_COMPLETE;
import static me.marcosassuncao.servsim.SimEvent.Type.TASK_START;

/**
 * Filter used to remove the simulation event that marks the
 * start and completion of jobs.
 * 
 * @author Marcos Dias de Assuncao
 */
class FilterJobEventsByIDs implements EventFilter<SimEvent>  {
	private HashSet<Integer> jobsId = new HashSet<>();
	
	/**
	 * Sets the reference time
	 * @param time the reference time for scanning the events
	 */
	public void setJobIDs(Collection<Integer> jobIds) {
		this.jobsId.addAll(jobIds);
	}
	
	/**
	 * Sets the ID of the job to be cancelled
	 * @param jobId the ID of the job to be cancelled
	 */
	public void setJobID(int jobId) {
		this.jobsId.add(jobId);
	}

	@Override
	public boolean test(SimEvent ev) {
		return (ev.type() == TASK_COMPLETE || 
				ev.type() == TASK_START) && 
					jobsId.contains(((WorkUnit)ev.content()).getId());
	}
}
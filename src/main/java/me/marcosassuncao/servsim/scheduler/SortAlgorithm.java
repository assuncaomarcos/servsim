package me.marcosassuncao.servsim.scheduler;

import static me.marcosassuncao.servsim.job.WorkUnit.Status.IN_EXECUTION;

import java.util.Comparator;

import me.marcosassuncao.servsim.job.Job;
import me.marcosassuncao.servsim.job.WorkUnit;

/**
 * An enumerator that maps some sorting algorithms 
 * to their corresponding job comparators.
 * 
 * @author Marcos Dias de Assuncao
 *
 */
public enum SortAlgorithm {
	/**
	 * Fist-In First-Out / FCFS queuing algorithm.
	 */
    FIFO ("First In, First Out", new OrderBySubmissionTime()),
	/**
	 * Algorithm that sorts jobs in decreasing order of priority.
	 */
    HPF ("Highest Priority First", new OrderByPriority()),
	/**
	 * Sorts jobs by deadline.
	 */
    EDF ("Earliest Deadline First", new OrderByDeadline());

    private final String description;
    private final Comparator<Job> comparator;
    
    SortAlgorithm (String description, Comparator<Job> comp) {
    	this.description = description;
    	this.comparator = comp;
    }
    
    /**
     * Returns a description of the algorithm.
     * @return a short description
     */
    public String getDescription() {
    	return description;
    }
    
    /**
     * Obtains the comparator used by the algorithm
     * @return a job comparator
     */
    public Comparator<Job> comparator() {
    	return comparator;
    }
}

/**
 * Comparator used to order a list of jobs according to 
 * their submission times 
 */
class OrderBySubmissionTime implements Comparator<Job> {
	public int compare(Job o1, Job o2) {
		return Long.compare(o1.getSubmitTime(), o2.getSubmitTime());
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}

/**
 * Comparator used to order a list of jobs according to their priorities. 
 * In case jobs of the same priority, break even as follows: 
 * <ul>
 * <li> If both jobs have the status {@link WorkUnit.Status#IN_EXECUTION},
 *  	then their submission times are used to break even.<br>
 *  	<b>NOTE:</b> This indicates that the comparator is being used 
 *  	to order a running queue.
 * <li> If only one of the jobs has the status {@link WorkUnit.Status#IN_EXECUTION},
 *  	then they are considered equal.<br>
 *  	<b>NOTE:</b> This indicates that the comparator is being used 
 *  	to select a job for preemption.
 * <li> If none of the jobs has the status {@link WorkUnit.Status#IN_EXECUTION},
 * 		then their submission times are used to break even.<br>
 * 		<b>NOTE:</b> This indicates that the comparator is being used to 
 * 		order a waiting queue.
 * </ul>
 */
class OrderByPriority implements Comparator<Job> {
	public int compare(Job o1, Job o2) {
		int comp = Integer.compare(o1.getPriority(), o2.getPriority());
		if (comp != 0) {
        	return comp;	
        } else {
        	if (o1.getStatus() == IN_EXECUTION && o2.getStatus() == IN_EXECUTION) {
        		return Long.compare(o1.getSubmitTime(), o2.getSubmitTime());
        	} else if (o1.getStatus() == IN_EXECUTION || o2.getStatus() == IN_EXECUTION) {
            	return 0;
        	} else {
        		return Long.compare(o1.getSubmitTime(), o2.getSubmitTime());
        	}
        }
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}

/** 
 * Comparator used to order a list of jobs 
 * according to their deadlines 
 */
class OrderByDeadline implements Comparator<Job> {
	public int compare(Job o1, Job o2) {
		return Long.compare(o1.getSubmitTime() + o1.getDeadlineDuration(),
							o2.getSubmitTime() + o2.getDeadlineDuration());
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
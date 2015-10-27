package me.marcosassuncao.servsim.profile;

import java.util.function.Predicate;

import me.marcosassuncao.servsim.job.WorkUnit;

/**
 * This interface is used to filter what jobs/reservations should be put 
 * in a given partition by policies that use multiple partitions or queues. 
 *  
 * @author Marcos Dias de Assuncao
 * 
 * @see ResourcePartition
 */

public interface PartitionPredicate extends Predicate<WorkUnit> {

	/**
	 * Checks whether a given job meets the criteria of the partition.
	 * @param item the job/reservation to be considered for scheduling.
	 * @return <code>true</code> if the job can be included in this
	 * partition; <code>false</code> otherwise.
	 */
	boolean test(WorkUnit item);
	
}

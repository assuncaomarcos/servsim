package me.marcosassuncao.servsim.profile;

/**
 * This class represents a resource partition for a multiple-partition
 * based availability profile.
 * 
 * @author Marcos Dias de Assuncao
 * 
 * @see PartProfile
 * @see PartitionPredicate
 */

public class ResourcePartition {
   	private int partId; 	// this partition's identifier
	private int numPEs; 	// the initial number of PEs given to the partition

	// to check which jobs can be scheduled in this partition
	private PartitionPredicate predicate;
	
	/**
	 * Creates a new <tt>QueuePartition</tt> object.
	 * @param queueId the partition ID
	 * @param numPE the number of PEs initially assigned to the partition 
	 * @param predicate the queue predicate
	 */
   	public ResourcePartition(int queueId, int numPE, 
   			PartitionPredicate predicate) {
   		this.partId = queueId;
   		numPEs = numPE;
   		this.predicate = predicate;
   	}

   	/**
   	 * Gets the partition ID 
   	 * @return the partition ID
   	 */
	public final int getPartitionId() {
		return partId;
	}
	
	/**
	 * Gets the number of PEs initially assigned to the partition
	 * @return the number of PEs initially assigned to the partition
	 */
	public final int getInitialNumResources() {
		return numPEs;
	}

	/**
	 * Gets the predicate of this partition
	 * @return the predicate of this partition
	 */
	public final PartitionPredicate getPredicate() {
		return predicate;
	}
}
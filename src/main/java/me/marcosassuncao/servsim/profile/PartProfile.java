package me.marcosassuncao.servsim.profile;

import me.marcosassuncao.servsim.job.WorkUnit;

import java.util.*;

import static com.google.common.base.Preconditions.checkElementIndex;

/**
 * This class represents the profile containing the ranges of PEs
 * available at given simulation times. This class is different from 
 * {@link SingleProfile} by the fact that it controls the availability
 * at multiple resource partitions.
 * 
 * @author Marcos Dias de Assuncao
 * 
 * @see Profile
 * @see PartProfileEntry
 * @see RangeList
 * @see TimeSlot
 * @see ResourcePartition
 */

public class PartProfile extends Profile<PartProfileEntry> implements Iterable<PartProfileEntry> {
	private ResourcePartition[] partitions;
	
	/**
	 * Creates a new {@link PartProfile} object. This constructor receives a
	 * collection of {@link ResourcePartition} objects which contain the IDs
	 * of the partitions and the initial assignments of processing elements. 
	 * @param parts the collection of resource partitions.
	 * @see ResourcePartition
	 * @see PartitionPredicate
	 */
	public PartProfile(ResourcePartition[] parts) {
		this(parts.length);
		PartProfileEntry fe = new PartProfileEntry(0L, parts.length);
		int firstPE = 0;
		int lastPE;
		
		for (ResourcePartition part: parts) {
			int partId = part.getPartitionId();
			if(partId >= partitions.length || partId < 0) {
				throw new IndexOutOfBoundsException("It is not possible to " +
						"add a partition with index: " + partId + ".");
			}
			else {
				partitions[partId] = part;
			}
			lastPE = firstPE + part.getInitialNumResources() - 1;
			RangeList pesPart = new RangeList(firstPE, lastPE);
			fe.setAvailRanges(partId, pesPart);
			firstPE = lastPE + 1;
		}
		
		super.add(fe);
	}
	
	/**
	 * Protected constructor used by the cloning operations.
	 * @param avail the availability information map.
	 * @param parts the array containing the resource partition objects.
	 * @see PartProfileEntry
	 */
	private PartProfile(LinkedTreeMap<Long,PartProfileEntry> avail,
										ResourcePartition[] parts) {
		this.avail.putAll(avail);
		partitions = new ResourcePartition[parts.length];
		
		for(int i=0; i<parts.length; i++) {
			partitions[i] = parts[i];
		}
	}
	
	/**
	 * Creates a new {@link PartProfile} object.<br>
	 * <b>NOTE:</b> if you use this constructor, you will have to add an initial
	 * entry to the profile.
	 * @param numParts the number of partitions in this profile.
	 */
	private PartProfile(int numParts) {
		partitions = new ResourcePartition[numParts];
	}
	
	/**
	 * Returns a shallow copy of this object.<br>
	 * <b>NOTE:</b> this method does not clone the entries.
	 * @return the cloned object
	 * @see PartProfile#copy()
	 */
	public PartProfile clone() {
		return new PartProfile(avail, partitions);
	}
	
	/**
	 * Returns a copy of this object.<br>
	 * <b>NOTE:</b> this method clones the entries, but does not clone the
	 * partition and predicates information.
	 * @return the copy object
	 */
	public PartProfile copy() {
		PartProfile copy = new PartProfile(partitions.length);
		copy.partitions = partitions;
		avail.forEach((time, entry) -> copy.add(entry.clone(time)));
		return copy;
	}
	
	/**
	 * Returns the ID of the partition whose predicate matches the schedule
	 * item provided. The method will return <code>-1</code> if no partition can
	 * handle the job/reservation.
	 * @param item the item to be scheduled.
	 * @return the partition ID or <code>-1</code> if no partition 
	 * can handle the job.
	 */
	public int matchPartition(WorkUnit item) {
		for (ResourcePartition part: partitions) {
			if(part.getPredicate().test(item)) {
				return part.getPartitionId();
			}
		}
		return -1;
	}
	
	/**
	 * Returns a profile entry with the available resources at a given
	 * partition at a given time. It does not scan the profile to check 
	 * if the resources will be available until the completion of a given job. 
	 * It just returns an entry with the resources available at the given time.
	 * @param partId the partition from which the ranges will be obtained
	 * @param time the time from which availability is checked
	 * 
	 * @return a {@link ProfileEntry} with the start time equals to the current 
	 * time and the ranges available at the current time.
	 */
	public ProfileEntry checkPartAvailability(int partId, long time) {
		Map.Entry<Long, PartProfileEntry> entry = avail.getPrecEntry(time, true);
		if (entry != null) {
			return new Entry(time, entry.getValue().getAvailRanges(partId));
		}
		else {
			return new Entry(time);
		}
	}
	
	/**
	 * Returns a profile entry if a given job with the characteristics
	 * provided can be scheduled. It will return <code>null</code> if it is not 
	 * possible to schedule the job.
	 * @param partId the id of the partition in which the job will be scheduled.
	 * @param startTime the start time of the job/reservation
	 * @param duration the duration of the job/reservation
	 * 
	 * @return a {@link ProfileEntry} with the start time provided and the 
	 * ranges available at that time + the duration.
	 */
	public ProfileEntry checkPartAvailability(int partId,  
							long startTime, long duration) {
		
		checkElementIndex(partId, partitions.length, 
				"Partition " + partId + " does not exist");
		
		Iterator<PartProfileEntry> it = avail.itValuesFromPrec(startTime);
		if (!it.hasNext()) {
			return new Entry(startTime);
		}
		
		PartProfileEntry entry = it.next();
		RangeList intersec = entry.getAvailRanges(partId).clone(); 
        long finishTime = startTime + duration;
            
        // Scans the availability profile until the expected termination
        // of the job to check whether enough PEs will be available for it. 
        while (it.hasNext()) {
        	entry = it.next();
        	if (entry.getTime() >= finishTime || intersec.getNumItems() == 0) {
        		break;
        	}
        	intersec = intersec.intersection(entry.getAvailRanges(partId));
        }
        
        return new Entry(startTime, intersec);
	}
	
	/**
	 * Selects an entry able to provide enough resources to handle a job. 
	 * The availability is checked considering that a job/reservation cannot
	 * start before {@code readyTime}. The method iterates the profile 
	 * starting at {@code readyTime} until it finds enough resources 
	 * for the job.
	 * @param partId the partition in which the job will be scheduled.
	 * @param reqPE the number of PEs
	 * @param readyTime entries prior to ready time will not be considered
	 * @param duration the duration in seconds to execute the job
	 * 
	 * @return an {@link ProfileEntry} with the time at which the job can start
	 * and the ranges available at that time.
	 */
	public ProfileEntry findPartStartTime(int partId, int reqPE, long readyTime, long duration) {
		checkElementIndex(partId, partitions.length, 
				"Partition " + partId + " does not exist");
		
		Iterator<PartProfileEntry> it = avail.itValuesFromPrec(readyTime);
        RangeList intersect = null;

        long potStartTime = readyTime;
        long potFinishTime;
        PartProfileEntry anchor;
        
        // scans entries until enough PEs are found
        while (it.hasNext()) { 
          	anchor = it.next();
           	if (anchor.getNumResources(partId) < reqPE) { 	
           		continue;
           	}
           	
           	potStartTime = Math.max(readyTime, anchor.getTime());
			potFinishTime = potStartTime + duration;
			intersect = anchor.getAvailRanges(partId);
			Iterator<PartProfileEntry> ita = avail.itValuesAfter(potStartTime);
			
			// Now scan the profile from potStartTime onwards analysing the
			// intersection of the ranges available in the entries until the
			// job's expected completion time.
			while (ita.hasNext()) {
				PartProfileEntry nextEntry = ita.next();
				if (nextEntry.getTime() >= potFinishTime) {
					break;
				}

				RangeList nextRanges = nextEntry.getAvailRanges(partId);
				if (nextRanges.getNumItems() < reqPE) {
					intersect = null;
					break;
				}

				intersect = intersect.intersection(nextEntry.getAvailRanges(partId));
				if (intersect.getNumItems() < reqPE) {
					break;
				}
			}

			if (intersect != null && intersect.getNumItems() >= reqPE) {
				break;
			}
        }
        
        if(intersect == null || intersect.getNumItems() < reqPE) {
        	return null;
        }
        
        return new Entry(potStartTime, intersect.clone());
	}
	
	/**
	 * Allocates a list of PE ranges from a partition to a job/reservation and 
	 * updates the availability profile accordingly. If the time of the last 
	 * entry is equals to finish time than another entry is not required. 
	 * In this case we just increase the number of jobs that rely on 
	 * that entry to mark either its completion time or start time. 
	 * The same is valid to the anchor entry, that is the entry that 
	 * represents the job's start time.
	 * @param partId the partition in which the job will be scheduled.
	 * @param selected the list of PE ranges selected
	 * @param startTime the start time of the job/reservation
	 * @param finishTime the finish time of the job/reservation
	 * 
	 * @see RangeList
	 */
	public void allocatePartResourceRanges(int partId, RangeList selected, 
			long startTime, long finishTime) {
		
		checkElementIndex(partId, partitions.length, 
				"Partition " + partId + " does not exist");

		Iterator<PartProfileEntry> it = avail.itValuesFromPrec(startTime);
		PartProfileEntry last = it.next();
		PartProfileEntry newAnchor = null;
        
        // The following is to avoid having to iterate the 
        // profile more than one time to update the entries. 
        if(last.getTime() == startTime) {
        	last.increaseJob();
        } else {
        	newAnchor = last.clone(startTime);
        	last = newAnchor;
        }

        PartProfileEntry nextEntry;
        while (it.hasNext()) {
       		nextEntry = it.next();
       		if (nextEntry.getTime() <= finishTime) {
       			last.getAvailRanges(partId).remove(selected);
       			last = nextEntry;
       			continue;
       		}
   			break;
        }

        if(last.getTime() == finishTime) {
        	last.increaseJob();
        }
        else {
        	add(last.clone(finishTime));
        	last.getAvailRanges(partId).remove(selected);
        }
        
        if(newAnchor != null) {
        	add(newAnchor);
        }
	}
	
	/**
	 * Returns the time slots contained in a given partition of this 
	 * availability profile within a specified period of time. <br>
	 * <b>NOTE:</b> The time slots returned by this method do not overlap.
	 * That is, they are not the scheduling options of a given job. They are
	 * the windows of availability. Also, they are sorted by start time.
	 * For example, obtaining the free time slots of partition 0 of the 
	 * scheduling queue below will result in time slots 1, 3 and 3: <br>
	 * <pre><br>
	 *   |
	 *   |                                       
	 *   |------------------                    
	 *   |                 |                    Part. 1
	 *   |      Job 5      |                         
	 * P |                 |                       
	 * E |===================================== 
  	 * s |    Job 3     |     Time Slot 3     | 
  	 *   |------------------------------------- 
	 *   |    Job 2  |      Time Slot 2       | Part. 0
	 *   |------------------------------------- 
  	 *   |  Job 1 |  Time Slot 1  |   Job 4   | 
  	 *   +------------------------------------- 
	 *  Start             Time              Finish 
	 *  Time                                 Time 
	 * <br></pre>
	 * 
	 * @param partId the partition from which the time slots are obtained.
	 * @param startTime the start time of the period.
	 * @param finishTime the finish time of the period.
	 * @return a collection with the time slots contained in the given 
	 * partition of the this availability information object within a 
	 * specified period of time.
	 */
	public Collection<TimeSlot> getPartTimeSlots(int partId, long startTime, 
			long finishTime) {
		if(partId >= partitions.length || partId < 0) {
			throw new IndexOutOfBoundsException("Partition " + partId + 
					" does not exist.");
		}
		ArrayList<Entry> subProfile = toArrayList(partId, startTime, finishTime);
		return super.getTimeSlots(finishTime, subProfile);
	}
	
	/**
	 * Returns the scheduling options for a job in a giving partition of this 
	 * availability profile within the specified period of time. 
	 * <b>NOTE:</b> In contrast to {@link #getPartTimeSlots(int, long, long)},
	 * the time slots returned by this method <b>OVERLAP</b> because they are 
	 * the scheduling options for jobs.
	 * 
	 * @param partId the id of the partition from which the scheduling options 
	 * will be obtained.
	 * @param startTime the start time of the period.
	 * @param finishTime the finish time of the period.
	 * @param duration the minimum duration of the free time slots. Free time 
	 * slots whose time frames are smaller than <code>duration</code> will be ignored.
	 * If you choose <code>1</code>, then all scheduling options will be returned.
	 * @param reqPEs the minimum number of PEs of the free time slots. Free 
	 * time slots whose numbers of PEs are smaller than <code>numPEs</code> will be 
	 * ignored. If you choose <code>1</code>, then all scheduling options will be returned.
	 * @return a collection with the time scheduling options in this availability 
	 * information object within the specified period of time.
	 */
	public Collection<TimeSlot> getPartSchedulingOptions(int partId, long startTime, 
								long finishTime, int duration, int reqPEs) {
		checkElementIndex(partId, partitions.length, 
				"Partition " + partId + " does not exist");
		
		ArrayList<TimeSlot> slots = new ArrayList<>();
		Iterator<PartProfileEntry> it = avail.itValuesFromPrec(startTime);
		PartProfileEntry ent;
		PartProfileEntry nxtEnt;
		RangeList slRgs;
		RangeList its;

		while (it.hasNext()) {
			ent = it.next();
			if (ent.getTime() >= finishTime) {
				break;
			} else if (ent.getNumResources(partId) == 0) {
				continue;
			}

			slRgs = ent.getAvailRanges(partId);
			long sStart = Math.max(ent.getTime(), startTime);

			while (slRgs != null && slRgs.getNumItems() > 0) {
				int initialPE = slRgs.getNumItems();
				Iterator<PartProfileEntry> ita = avail.itValuesAfter(sStart);
				boolean changed = false;
				
				while (ita.hasNext() && !changed) {
					nxtEnt = ita.next();
					
					if (nxtEnt.getTime() >= finishTime) {
						break;
					}
					
					its = slRgs.intersection(nxtEnt.getAvailRanges(partId));
					if (its.getNumItems() == slRgs.getNumItems()) {
						continue;
					}
					 
					// if there was a change in the number of PEs, so that less 
					// PEs are available after the next entry, then considers 
					// the next entry as the end of the current time slot
					long slEnd = Math.min(nxtEnt.getTime(), finishTime);
					if ((slEnd - sStart) >= duration && slRgs.getNumItems() >= reqPEs) {
						TimeSlot slot = new TimeSlot(sStart, slEnd, slRgs.clone());
						slots.add(slot);
					}
					changed = true;
					slRgs = its;
				}

				if (slRgs.getNumItems() == initialPE) {
					if ((finishTime - sStart) >= duration && slRgs.getNumItems() >= reqPEs) {
						TimeSlot slot = new TimeSlot(sStart, finishTime, slRgs.clone());
						slots.add(slot);
					}
					
					slRgs = null;
				}
			} 
		}
		return slots;
	}
	
	/**
	 * Includes a time slot in this availability profile. This is useful if 
	 * your scheduling strategy cancels a job and you want to update the 
	 * availability profile.
	 * @param partId the partition to which the time slot will be added.
	 * @param startTime the start time of the time slot.
	 * @param finishTime the finish time of the time slot.
	 * @param list the list of ranges of PEs in the slot.
	 * @return <code>true</code> if the slot was included; <code>false</code> otherwise.
	 */
	public boolean addPartTimeSlot(int partId, long startTime, 
										long finishTime, RangeList list) {

		if (finishTime <= startTime) {
			return false;
		}
		
		Iterator<PartProfileEntry> it = avail.itValuesFromPrec(startTime);
        PartProfileEntry last = it.next();
        PartProfileEntry newAnchor = null;

		// Redundant entries can be removed only if their time is greater than
		// current simulation clock because one entry before or at the
		// simulation clock time is required as the starting point of the profile
        if (last.getTime() == startTime) {
        	if (last.decreaseJob() <= 0) {
        		it.remove();
        	}
        } else {
        	newAnchor = last.clone(startTime);
        	last = newAnchor;
        }

        PartProfileEntry nextEntry = null;
        while (it.hasNext()) {
       		nextEntry = it.next();
       		if(nextEntry.getTime() <= finishTime) {
       			last.getAvailRanges().remove(list);
       			last.getAvailRanges(partId).addAll(list.clone());
       			last = nextEntry;
       			continue;
       		}
   			break;
        }

        if (last.getTime() == finishTime) {
        	if(last.decreaseJob() <= 0) {
        		it.remove();
        	}
        } else {
        	add(last.clone(finishTime));
        	last.getAvailRanges().remove(list);
        	last.getAvailRanges(partId).addAll(list.clone());
        }
        
        if(newAnchor != null) {
        	add(newAnchor);
        }
        
        return true;
	}
	
	/**
	 * Creates an string representation of the profile
	 * @return an string representation
	 */
	public String toString() {
		StringBuilder result = new StringBuilder("Profile={\n");
		for(ProfileEntry entry : avail.values()){
			result.append(entry);
			result.append("\n");
		}
		result.append("}");
		return result.toString();
	}
	
	/**
	 * Returns an iterator in case someone needs to iterate this
	 * object. <br>
	 * <b>NOTE:</b> Removing objects from this profile via its iterator
	 * may make it behave in an unexpected way.
	 *  
	 * @return an iterator for the {@link SingleProfileEntry} objects in this profile.
	 */
	public Iterator<PartProfileEntry> iterator() {
		return new PrivateValueIterator();
	}
	
	/**
	 * Returns the part of the availability profile's entries in an 
	 * {@link ArrayList}. It returns only the ranges of a given partition.<br>
	 * <b>NOTE:</b> The entries of the sub-profile are clones of the original 
	 * profile object's entries. Therefore, the changes made to the object 
	 * returned by this method will not impact this availability profile.
	 * @param partId the partition of interest
	 * @param startTime the start time of the resulting part
	 * @param finishTime the finish time of the resulting part
	 * 
	 * @return part of the availability profile. 
	 */    
	private ArrayList<Entry> toArrayList(int partId, long startTime, long finishTime) {
		checkElementIndex(partId, partitions.length, 
				"Partition " + partId + " does not exist");
		
		ArrayList<Entry> subProfile = new ArrayList<>();
		Iterator<PartProfileEntry> it = avail.itValuesFromPrec(startTime);
		Entry fe;

		// get first entry or create one if the profile is empty
		if (it.hasNext()) {
			PartProfileEntry ent = it.next();
			RangeList list = ent.getAvailRanges(partId) == null ? 
					null : ent.getAvailRanges(partId).clone();
			long entTime = Math.max(startTime, ent.getTime());
			fe = new Entry(entTime, list);
		} else {
			fe = new Entry(startTime);
		}
		subProfile.add(fe);
		
		while (it.hasNext()) {
			PartProfileEntry entry = it.next();
           	if (entry.getTime() > finishTime) {
           		break;
   			}
           	
			RangeList list = entry.getAvailRanges(partId) == null ? 
					null : entry.getAvailRanges(partId).clone();
           	
			Entry newEntry = new Entry(entry.getTime(), list);
			subProfile.add(newEntry);
        }

		return subProfile;
    }
	
	/**
	 * A delegation based iterator in case someone needs to iterate this
	 * object. <br>
	 * <b>NOTE:</b> Removing objects from this profile via its iterator
	 * may make it behave in an unexpected way.
	 * 
	 * @author Marcos Dias de Assuncao
	 */
	private class PrivateValueIterator implements Iterator<PartProfileEntry> {
		private final Iterator<PartProfileEntry> it;

        PrivateValueIterator() {
        	it = avail.values().iterator();
	    }

	    public boolean hasNext() {
	    	return it.hasNext();
	    }

	    public PartProfileEntry next() {
	    	if (!it.hasNext()) {
	    		throw new NoSuchElementException("Element does not exist");
	    	}
	    	return it.next();
	    }

	    public void remove() {
	    	it.remove();
	    }
	}
}
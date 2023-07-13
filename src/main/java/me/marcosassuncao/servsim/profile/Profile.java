package me.marcosassuncao.servsim.profile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.base.MoreObjects;

/**
 * This class represents the profile containing the ranges of PEs
 * available at given simulation times.
 *
 * @author Marcos Dias de Assuncao
 */

public abstract class Profile<V extends ProfileEntry> {
	/**
	 * Data structure used to track resource usage.
	 */
	protected LinkedTreeMap<Long,V> avail = new LinkedTreeMap<>();

	/**
	 * Protected constructor.
	 */
	protected Profile() {}

	/**
	 * Protected constructor used by the cloning operations.
	 * @param avail the availability information map.
	 * @see ProfileEntry
	 */
	protected Profile(TreeMap<Long,V> avail) {
		this.avail.putAll(avail);
	}

	/**
	 * Removes past entries from the availability profile, but keeps the
	 * entry corresponding to the reference time provided, or the entry preceding
	 * it if an entry with the provided time does not exist.
	 * @param refTime the reference time for removing the entries. <br>
	 */
	public void removePastEntries(long refTime) {
		long timePrec = getPrecedingValue(refTime).getTime();
		Iterator<Long> it = avail.keySet().iterator();
		while (it.hasNext()) {
			long timeEntry = it.next();
			if (timeEntry >= timePrec){
				break;
			}
			it.remove();
		}
	}

	/**
	 * Returns a profile entry with the available resources at a given time.
	 * @param time the time from which the availability is checked
	 * @return a {@link ProfileEntry} with the ranges available at the given time.
	 */
	public ProfileEntry checkAvailability(long time) {
		Map.Entry<Long, V> entry = avail.getPrecEntry(time, true);
		return entry == null ?
				new Entry(time) : entry.getValue().clone(time);
	}

	/**
	 * Returns a profile entry if a given job with the characteristics
	 * provided can be scheduled.
	 * @param reqRes the number of resources.
	 * @param startTime the start time of the job/reservation
	 * @param duration the duration of the job/reservation
	 * @param acceptLess defines whether less resources than originally requested is allowed
	 * @return a {@link ProfileEntry} with the start time provided and the
	 * ranges available at that time OR <code>null</code> if not enough resources are found.
	 */
	public ProfileEntry checkAvailability(int reqRes, long startTime, long duration, boolean acceptLess) {

		Iterator<V> it = avail.itValuesFromPrec(startTime);
		if (!it.hasNext()) {
			return null;
		}

		RangeList intersec = it.next().getAvailRanges().clone();
        long finishTime = startTime + duration;

        // Scans the availability profile until the expected termination
        // of the job to check whether enough PEs will be available for it.
        while (it.hasNext()) {
        	ProfileEntry entry = it.next();
        	if (entry.getTime() >= finishTime || (!acceptLess && intersec.getNumItems() < reqRes)) {
        		break;
        	}
        	intersec = intersec.intersection(entry.getAvailRanges());
        }

        return (intersec.getNumItems() >= reqRes) || acceptLess ?
        		new Entry(startTime, intersec) : null;
	}


	/**
	 * Returns a profile entry if a given job with the characteristics
	 * provided can be scheduled.
	 * @param reqRes the number of resources.
	 * @param startTime the start time of the job/reservation
	 * @param duration the duration of the job/reservation
	 * @return a {@link ProfileEntry} with the start time provided and the
	 * ranges available at that time OR <code>null</code> if not enough resources are found.
	 */
	public ProfileEntry checkAvailability(int reqRes, long startTime, long duration) {
		return checkAvailability(reqRes, startTime, duration, false);
	}

	/**
	 * Selects an entry able to provide enough processors to handle a job. The method
	 * iterates the profile until it finds enough processors for the job, starting
	 * from the current simulation time.
	 * @param reqPE the number of processors
	 * @param readyTime entries prior to ready time will not be considered
	 * @param duration the duration in seconds to execute the job
	 * @return a {@link ProfileEntry} with the time at which the job can start
	 * and the ranges available at that time.
	 */
	public ProfileEntry findStartTime(int reqPE, long readyTime, long duration) {
		Iterator<V> it = avail.itValuesFromPrec(readyTime);
        RangeList intersect = null;
        long potStartTime = readyTime;
        long potFinishTime;
        ProfileEntry anchor;

       	// scans the profile until an entry with enough PEs is found
        while (it.hasNext()) {
          	anchor = it.next();
           	if (anchor.getNumResources() < reqPE) {
           		continue;
           	}

           	potStartTime = Math.max(readyTime, anchor.getTime());
			potFinishTime = potStartTime + duration;
			intersect = anchor.getAvailRanges();
			Iterator<V> ita = avail.itValuesAfter(potStartTime);

			// Now scan the profile from potStartTime onwards analysing the
			// intersection of the ranges available in the entries until the
			// job's expected completion time.
			while (ita.hasNext()) {
				ProfileEntry nextEntry = ita.next();
				if (nextEntry.getTime() >= potFinishTime) {
					break;
				}

				RangeList nextRanges = nextEntry.getAvailRanges();
				if (nextRanges.getNumItems() < reqPE) {
					intersect = null;
					break;
				}

				intersect = intersect.intersection(nextEntry.getAvailRanges());
				if (intersect.getNumItems() < reqPE) {
					break;
				}
			}
			if (intersect != null && intersect.getNumItems() >= reqPE) {
				break;
			}
        }

        if (intersect == null || intersect.getNumItems() < reqPE) {
        	return null;
        }

        return new Entry(potStartTime, intersect.clone());
	}

	/**
	 * Allocates a list of resource ranges to a job/reservation.
	 * @param selected the list of resource ranges selected
	 * @param startTime the start time of the job/reservation
	 * @param finishTime the finish time of the job/reservation
	 */
	@SuppressWarnings("unchecked")
	public void allocateResourceRanges(RangeList selected,
			long startTime, long finishTime) {

		// If the time of the last entry is equals to finish time then another
		// entry is not required. In this case we just increase the number of
		// work units that rely on that entry to mark either its completion
		// time or start time. The same is valid to the anchor entry, that is
		// the entry that represents the job's start time.
		Iterator<V> it = avail.itValuesFromPrec(startTime);
		V last = it.next();
		V newAnchor = null;

        if (last.getTime() == startTime) {
        	last.increaseJob();
        } else {
        	newAnchor = (V)last.clone(startTime);
        	last = newAnchor;
        }

        V nextEntry;
        while (it.hasNext()) {
       		nextEntry = it.next();
       		if (nextEntry.getTime() <= finishTime) {
       			last.getAvailRanges().remove(selected);
       			last = nextEntry;
       			continue;
       		}
   			break;
        }

        if (last.getTime() == finishTime) {
        	last.increaseJob();
        } else {
        	add((V)last.clone(finishTime));
        	last.getAvailRanges().remove(selected);
        }

        if (newAnchor != null) {
        	add(newAnchor);
        }
	}

	/**
	 * Returns the time slots contained in this availability profile
	 * within a specified period of time. <br>
	 * <b>NOTE:</b> The time slots returned by this method do not overlap.
	 * That is, they are not the scheduling options of a given job. They are
	 * the windows of availability. Also, they are sorted by start time.
	 * For example: <br>
	 * <pre><br>
	 *   |-------------------------------------
  	 *   |    Job 3     |     Time Slot 3     |
  	 * P |-------------------------------------
	 * E |    Job 2  |      Time Slot 2       |
	 * s |-------------------------------------
  	 *   |  Job 1 |  Time Slot 1  |   Job 4   |
  	 *   +-------------------------------------
	 *  Start             Time              Finish
	 *  Time                                 Time
	 * <br></pre>
	 * @param startTime the start time of the period.
	 * @param finishTime the finish time of the period.
	 * @return a collection with the time slots.
	 * @see TimeSlot
	 */
	public Collection<TimeSlot> getTimeSlots(long startTime, long finishTime) {
		ArrayList<Entry> subProfile = toArrayList(startTime, finishTime);
		return getTimeSlots(finishTime, subProfile);
	}

	/**
	 * Returns the scheduling options of this availability profile within the
	 * specified period of time.
	 * <b>NOTE:</b> The time slots returned by this method <b>OVERLAP</b>
	 * because they are the scheduling options for jobs.
	 * @param startTime the start time of the period.
	 * @param finishTime the finish time of the period.
	 * @param duration the minimum duration of the free time slots. Free time
	 * slots whose time frames are smaller than <code>duration</code> will be ignored.
	 * If you choose {@code 1}, then all scheduling options will be returned.
	 * @param reqPEs the minimum number of processors of the free time slots. Free
	 * time slots whose numbers of processors are smaller than {@code reqPEs} will be
	 * ignored. If you choose {@code 1}, then all scheduling options will be returned.
	 *
	 * @return a collection with the time scheduling options.
	 * @see TimeSlot
	 */
	public Collection<TimeSlot> getSchedulingOptions(long startTime,
			long finishTime, int duration, int reqPEs) {
		ArrayList<TimeSlot> slots = new ArrayList<>();

		Iterator<V> it = avail.itValuesFromPrec(startTime);
		ProfileEntry ent;
		ProfileEntry nxtEnt;
		RangeList slRgs;
		RangeList its;

		while(it.hasNext()) {
			ent = it.next();
			if(ent.getTime() >= finishTime) {
				break;
			} else if (ent.getNumResources() == 0) {
				continue;
			}

			slRgs = ent.getAvailRanges();
			long sStart = Math.max(ent.getTime(), startTime);

			while (slRgs != null && slRgs.getNumItems() > 0) {
				int initialPE = slRgs.getNumItems();
				Iterator<V> ita = avail.itValuesAfter(startTime);
				boolean changed = false;

				while (ita.hasNext() && !changed) {
					nxtEnt = ita.next();

					if (nxtEnt.getTime() >= finishTime) {
						break;
					}

					its = slRgs.intersection(nxtEnt.getAvailRanges());
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

				if(slRgs.getNumItems() == initialPE) {
					if((finishTime - sStart) >= duration && slRgs.getNumItems() >= reqPEs) {
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
	 * Creates an string representation of the profile
	 * @return an string representation
	 */
	public String toString() {
		return MoreObjects.toStringHelper(Profile.class)
				.add("entries", avail.values()).toString();
	}

	// ------------------ PROTECTED METHODS -----------------------

	/**
	 * Adds an entry to the availability profile.
	 * @param entry the entry to be removed.
	 * @return the entry replaced by the new entry or {@code null} if no
	 * entry was replaced.
	 */
	protected ProfileEntry add(V entry) {
		return avail.put(entry.getTime(), entry);
	}

	/**
	 * Returns the entry whose time is closest to the {@code time} given but
	 * smaller, or whose time is equals to {@code time}
	 * @param time the time to be used to search for the entry
	 * @return the entry whose time is closest to the {@code time} given but
	 * smaller, or whose time is equals to {@code time}; {@code null} if
	 * not found.
	 */
	protected ProfileEntry getPrecedingValue(long time) {
		Map.Entry<Long,V> entry = avail.getPrecEntry(time, true);
		return entry == null ? null : entry.getValue();
	}

	/**
	 * A helper method which actually does the real job for
	 * {@link Profile#getTimeSlots(long, long)}.
	 * @param finishTime the finish time of the period.
	 * @param subProfile the profile already cloned and cut from start time
	 * and finish time.
	 * @return a collection with the time slots.
	 */
	public Collection<TimeSlot> getTimeSlots(long finishTime, ArrayList<Entry> subProfile) {
		ArrayList<TimeSlot> slots = new ArrayList<>();

		long slStart;		// the start time of the slot
		long slEnd;			// the end time of the slot
		int stIdx;			// index in which a slot starts
		int endIdx;			// index in which a slot finishes

		ProfileEntry ent;
		ProfileEntry nxtEnt;
        RangeList slRgs;	// ranges of the slot
        RangeList its;		// the intersection of ranges
        int size = subProfile.size();

        for (int i=0; i<size; i++) {
        	ent = subProfile.get(i);

        	if (ent.getNumResources() == 0) {
        		continue;
        	}

        	slStart = ent.getTime();
        	stIdx = i;

        	// check all possible time slots starting at slStart
        	while (ent.getNumResources() > 0) {
        		slRgs = its = ent.getAvailRanges();
        		slEnd = finishTime;
				endIdx = stIdx;
	        	for(int j=i+1; j<size; j++) {
	        		nxtEnt = subProfile.get(j);
	        		its = its.intersection(nxtEnt.getAvailRanges());

	        		if(its.getNumItems() == 0) {
	        			slEnd = nxtEnt.getTime();
	        			break;
	        		}

	        		slRgs = its;
	        		endIdx = j;
	        	}

	        	// clone ranges because they may be pointing to ranges that
	        	// will be deleted next
	        	TimeSlot slot = new TimeSlot(slStart, slEnd, slRgs.clone());
	        	slots.add(slot);

	        	for (int j=stIdx; j<=endIdx; j++) {
	        		nxtEnt = subProfile.get(j);
	        		nxtEnt.getAvailRanges().remove(slRgs);
	        	}
        	}
        }

		return slots;
	}

	/**
	 * Return the a collection with the profile entries over a specified period
	 * @param startTime the start time for the query
	 * @param finishTime the finish time for the query
	 * @return a collection containing the entries
	 */
	public Collection<ProfileEntry> getAvailability(long startTime, long finishTime) {
		return new ArrayList<>(toArrayList(startTime, finishTime));
	}

	/**
	 * Returns part of the availability profile.<br>
	 * <b>NOTE:</b> The returned entries are clones of the original ones.
	 * @param startTime the start time of the resulting part
	 * @param finishTime the finish time of the resulting part
	 * @return part of the availability profile.
	 */
	public ArrayList<Entry> toArrayList(long startTime, long finishTime) {
		ArrayList<Entry> subProfile = new ArrayList<>();

		Iterator<V> it = avail.itValuesFromPrec(startTime);
		Entry fe;

		// get first entry or create one if the profile is empty
		if(it.hasNext()) {
			ProfileEntry ent = it.next();
			RangeList list = ent.getAvailRanges() == null ?
					null : ent.getAvailRanges().clone();
			long entTime = Math.max(startTime, ent.getTime());
			fe = new Entry(entTime, list);
		} else {
			fe = new Entry(startTime);
		}
		subProfile.add(fe);

		while (it.hasNext()) {
			ProfileEntry entry = it.next();
           	if(entry.getTime() > finishTime) {
           		break;
   			}

			RangeList list = entry.getAvailRanges() == null ?
					null : entry.getAvailRanges().clone();

			Entry newEntry = new Entry(entry.getTime(), list);
			subProfile.add(newEntry);
        }

		return subProfile;
    }

	/**
	 * This class is used to return an entry when the user calls one of
	 * the methods to query the availability of resources.
	 *
	 * @author Marcos Dias de Assuncao
	 */
	protected static class Entry extends ProfileEntry {
		private RangeList ranges;

		/**
		 * Creates an entry with null ranges and the time given
		 * @param time the time for the entry
		 */
		protected Entry(long time) {
			super(time);
			ranges = new RangeList();
		}

		/**
		 * Creates an entry with the list of ranges and the time given
		 * @param time the time for the entry
		 * @param list the list of ranges
		 */
		protected Entry(long time, RangeList list) {
			super(time);
			ranges = list;
		}

		@Override
		public RangeList getAvailRanges() {
			return ranges;
		}

		@Override
		public int getNumResources() {
			return ranges == null ? 0 : ranges.getNumItems();
		}

		@Override
		public ProfileEntry clone(long time) {
			Entry clone = new Entry(time);
			clone.ranges = ranges.clone();
			return clone;
		}

		public String toString() {
			return MoreObjects.toStringHelper(ProfileEntry.class)
				       .add("time", super.getTime())
				       .add("numProc", ranges != null ? ranges.getNumItems() : 0)
				       .add("ranges", ranges != null ? ranges : "{[]}").toString();
		}

		/**
		 * Create a profile entry with the provided time and resource ranges.
		 * @param time the time of the entry
		 * @param list the resource ranges
		 * @return the created entry
		 */
		public Entry create(long time, RangeList list) {
			return new Entry(time, list != null ? list.clone() : null);
		}
	}
}

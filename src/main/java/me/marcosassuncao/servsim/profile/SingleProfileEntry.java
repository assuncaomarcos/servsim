package me.marcosassuncao.servsim.profile;

import com.google.common.base.MoreObjects;

/**
 * The {@link SingleProfileEntry} class represents an entry in the availability 
 * profile. It contains the list of ranges of PEs available at a particular
 * time. This time may represent either the start time or completion of
 * a job or advance reservation.
 * 
 * @author  Marcos Dias de Assuncao
 * 
 * @see Range
 * @see RangeList
 * @see SingleProfile
 */

public class SingleProfileEntry extends ProfileEntry {
	private RangeList ranges;
	
	/**
	 * Creates a new instance of {@link SingleProfileEntry}
	 * @param time the time associated with this entry
	 */
	public SingleProfileEntry(long time) {
		super(time);
		ranges = null;
	}
	
	/**
	 * Creates a new instance of {@link SingleProfileEntry}
	 * @param time the time associated with this entry
	 * @param ranges the list of ranges of PEs available
	 */
	public SingleProfileEntry(long time, RangeList ranges) {
		super(time);
		this.ranges = ranges;
	}

	/**
	 * Returns the list of ranges available at this entry
	 * @return the list of ranges available
	 */
	public RangeList getAvailRanges() {
		return ranges;
	}

	/**
	 * Sets the ranges of PEs available at this entry
	 * @param availRanges the list of ranges of PEs available
	 */
	public void setAvailRanges(RangeList availRanges) {
		ranges = availRanges;
	}
	
	/**
	 * Adds the ranges provided to the list of ranges available 
	 * @param list the list to be added
	 * @return <tt>true</tt> if the ranges changed as result of this call
	 */
	public boolean addRanges(RangeList list) {
		return ranges.addAll(list == null ? new RangeList() : list);
	}

	/**
	 * Gets the number of PEs associated with this entry
	 * @return the number of PEs
	 */
	public int getNumResources() {
		return ranges == null ? 0 : ranges.getNumItems();
	}
		
	/**
	 * Creates a string representation of this entry
	 * @return a representation of this entry
	 */
	public String toString() {
		return MoreObjects.toStringHelper(ProfileEntry.class)
			       .add("time", super.getTime())
			       .add("numProc", ranges != null ? ranges.getNumItems() : 0)
			       .add("ranges", ranges != null ? ranges : "{[]}").toString();
	}
	
	/**
	 * Returns a clone of this entry. The ranges are cloned, but the time
	 * and the number of requests relying on this entry are not.
	 * @param time the time for the new entry
	 * @return the new entry with the number of requests set to default.
	 */
	public SingleProfileEntry clone(long time) {
		return new SingleProfileEntry(time, ranges == null ? null : ranges.clone());
	}
}

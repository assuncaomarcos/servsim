package me.marcosassuncao.servsim.profile;

/**
 * The {@link ProfileEntry} class represents an entry in the availability 
 * profile. It contains the list of ranges of processors available at a particular
 * time. This time may represent either the start time or completion of
 * a job or advance reservation.
 * 
 * @author  Marcos Dias de Assuncao
 * 
 * @see Range
 * @see RangeList
 */

public abstract class ProfileEntry implements Comparable<ProfileEntry> {
	private long time;
	
	// number of jobs that rely on this entry
	// to mark their completion time or anchor point
	private int numJobs = 1;	
	
	/**
	 * Creates a new instance of {@link ProfileEntry}
	 * @param time the time associated with this entry
	 */
	protected ProfileEntry(long time) {
		this.time = time;
	}
	
	/**
	 * Gets the number of processors associated with this entry
	 * @return the number of processors
	 */
	public abstract int getNumResources();
	
	/**
	 * Returns the list of ranges available at this entry
	 * @return the list of ranges available
	 */
	public abstract RangeList getAvailRanges();
	
	/**
	 * Returns a clone of this entry. The ranges are cloned, but the time
	 * and the number of requests relying on this entry are not.
	 * @param newTime the time for the new entry
	 * @return the new entry with the number of requests set to default.
	 */
	public abstract ProfileEntry clone(long newTime);
	
	/**
	 * Gets the time associated with this entry
	 * @return the time associated with this entry
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Sets the time associated with this entry
	 * @param time the time associated with this entry
	 * @return {@code true} if the time has been set successfully or
	 * {@code false} otherwise.
	 */
	public boolean setTime(long time) {
		if(time < 0) {
			return false;
		}
		
		this.time = time;
		return true;
	}

	/**
	 * Increases the number of jobs/reservations that rely on this entry to mark
	 * their expected completion time or their anchor point
	 * @return the number of jobs/reservations currently relying on this entry
	 */
	public int increaseJob() {
		return ++numJobs;
	}
	
	/**
	 * Decreases the number of jobs/reservations that rely on this entry to mark
	 * their expected completion time or their anchor point
	 * @return the number of jobs/reservations currently relying on this entry
	 */
	public int decreaseJob() {
		return --numJobs;
	}
	
	/**
	 * Returns the number of jobs/reservations that rely on this entry to mark
	 * their expected completion time or their anchor point
	 * @return the number of jobs/reservations that use this entry
	 */
	public int getNumJobs() {
		return numJobs;
	}
	
	/**
	 * Compares this object with the specified object for order. 
	 * Returns a negative integer, zero, or a positive integer 
	 * as this object is less than, equal to, or greater 
	 * than the specified object.
	 * @param entry the entry to be compared.
	 * @return a negative integer, zero, or a positive integer as 
	 * this entry is less than, equal to, or greater 
	 * than the specified entry.
	 */
	public int compareTo(ProfileEntry entry) {
		return Long.compare(time, entry.time);
	}
}

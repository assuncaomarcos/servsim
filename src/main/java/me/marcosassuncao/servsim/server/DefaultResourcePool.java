package me.marcosassuncao.servsim.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.google.common.base.MoreObjects;
import me.marcosassuncao.servsim.job.WorkUnit;
import me.marcosassuncao.servsim.profile.ProfileEntry;
import me.marcosassuncao.servsim.profile.RangeList;
import me.marcosassuncao.servsim.profile.SingleProfile;
import me.marcosassuncao.servsim.profile.TimeSlot;

/**
 * This class represents a pool of resources. This class implements
 * a delegation of {@link SingleProfile}.
 *
 * @author Marcos Dias de Assuncao
 */
public class DefaultResourcePool extends ResourcePool {
	/**
	 * The resource profile used to track resource usage.
	 */
	protected SingleProfile profile;

	/**
	 * Creates a new resource pool
	 * @param capacity the number of resources to create in the list
	 */
	public DefaultResourcePool(int capacity) {
		super(capacity);
		this.profile = new SingleProfile(capacity);
	}

	/**
	 * Returns a profile entry with the currently available resources.
	 * @param time the time from which the availability is checked
	 * @return a {@link ProfileEntry} with ranges available at the given time.
	 */
	public ProfileEntry checkAvailability(long time) {
		return this.profile.checkAvailability(time);
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
		return this.profile.checkAvailability(reqRes, startTime, duration);
	}

	/**
	 * Returns a profile entry if a given job with the characteristics
	 * provided can be scheduled.
	 * @param reqRes the number of resources.
	 * @param startTime the start time of the job/reservation
	 * @param duration the duration of the job/reservation
	 * @param acceptLessResources defines whether less resources than originally requested is allowed
	 * @return a {@link ProfileEntry} with the start time provided and the
	 * ranges available at that time OR <code>null</code> if not enough resources are found.
	 */
	public ProfileEntry checkAvailability(int reqRes, long startTime,
			long duration, boolean acceptLessResources) {
		return this.profile.checkAvailability(reqRes, startTime, duration, acceptLessResources);
	}

	/**
	 * Selects an entry able to provide enough resources to handle a job. The method
	 * iterates the profile until it finds enough resources for the job, starting
	 * from the current simulation time.
	 * @param reqRes the number of resources
	 * @param duration the duration in seconds to execute the job
	 * @return an {@link ProfileEntry} with the time at which the job can start
	 * and the ranges available at that time.
	 */
	public ProfileEntry findStartTime(int reqRes, long duration) {
		return findStartTime(reqRes, super.currentTime(), duration);
	}

	/**
	 * Selects an entry able to provide enough resources to handle a job. The method
	 * iterates the profile until it finds enough resources for the job, starting
	 * from the current simulation time.
	 * @param reqRes the number of resources
	 * @param readyTime the time before which a request cannot be scheduled
	 * @param duration the duration in seconds to execute the job
	 * @return an {@link ProfileEntry} with the time at which the job can start
	 * and the ranges available at that time.
	 */
	public ProfileEntry findStartTime(int reqRes, long readyTime, long duration) {
		return this.profile.findStartTime(reqRes, readyTime, duration);
	}

	/**
	 * Allocates a list of resource ranges to a job/reservation.
	 * @param task the work unit for which resources need to be allocated
	 * @param selected the list of resource ranges selected
	 * @param startTime the start time of the job/reservation
	 */
	public void allocateResources(WorkUnit task, RangeList selected, long startTime) {
		allocateResources(selected, startTime, startTime + task.getDuration());
	}

	/**
	 * Allocates a list of resource ranges to a job/reservation.
	 * @param selected the list of resource ranges selected
	 * @param startTime the start time of the job/reservation
	 * @param finishTime the finish time of the job/reservation
	 */
	public void allocateResources(RangeList selected, long startTime, long finishTime) {
		this.profile.allocateResourceRanges(selected, startTime, finishTime);
	}

	/**
	 * Includes a time slot in this availability profile. This is useful if
	 * your scheduling strategy cancels a job and you want to update the
	 * availability profile.
	 * @param startTime the start time of the time slot.
	 * @param finishTime the finish time of the time slot.
	 * @param list the list of ranges of resources in the slot.
	 * @return <code>true</code> if the slot was included; <code>false</code> otherwise.
	 */
	public boolean releaseResources(long startTime, long finishTime, RangeList list) {
		return this.profile.addTimeSlot(startTime, finishTime, list);
	}

	/**
	 * Returns the number of simulation units over which resources
	 * have been free. This is useful for computing resource utilisation.
	 * @param startTime start time to consider when computing the units
	 * @param endTime end time to consider when computing the units
	 * @return the number of free resource units
	 */
	public long getNumberFreeUnits(long startTime, long endTime) {
		Collection<ProfileEntry> avail = profile.getAvailability(startTime, endTime);
		long units = 0;
		Iterator<ProfileEntry> it = avail.iterator();
		ProfileEntry prev = it.next();

		while (it.hasNext()) {
			ProfileEntry curr = it.next();

			if (curr.getTime() < startTime) {
				prev = curr;
				continue;
			}

			if (curr.getTime() > endTime) {
				break;
			}

			units += (curr.getTime() - prev.getTime()) * prev.getNumResources();
			prev = curr;
		}

		units += (endTime - Math.min(endTime, prev.getTime())) * prev.getNumResources();
		return units;
	}

	/**
	 * Returns the number of simulation units over which resources
	 * have been used. This is useful for computing resource utilisation.
	 * @param startTime start time to consider when computing the units
	 * @param endTime end time to consider when computing the units
	 * @return the number of used resource units
	 */
	public long getNumberUsedUnits(long startTime, long endTime) {
		Collection<ProfileEntry> avail = profile.getAvailability(startTime, endTime);
		long units = 0;
		Iterator<ProfileEntry> it = avail.iterator();
		ProfileEntry prev = it.next();

		while (it.hasNext()) {
			ProfileEntry curr = it.next();

			if (curr.getTime() < startTime) {
				prev = curr;
				continue;
			}

			if (curr.getTime() > endTime) {
				break;
			}

			long time = curr.getTime() - Math.max(startTime, prev.getTime());
			units += time * (super.getCapacity() - prev.getNumResources());
			prev = curr;
		}

		long time = endTime - Math.max(startTime, prev.getTime());
		units += time * (super.getCapacity() - prev.getNumResources());
		return units;
	}

	/**
	 * Returns the resource utilisation during a given period
	 * @param startTime the initial time
	 * @param endTime the finish time
	 * @return the resource utilisation between <code>0.0</code> and <code>1.0</code>
	 */
	public double getUtilization(long startTime, long endTime) {
		long totalUnits = super.getCapacity() * (endTime - startTime);
		long usedUnits = totalUnits - getNumberFreeUnits(startTime, endTime);
		return ((double)usedUnits) / ((double)totalUnits);
	}

	/**
	 * Returns all the changes in resource usage over a specified period
	 * @param startTime the start of the period for the query
	 * @param finishTime the end time of the period for the query
	 * @return a collection of resource usage entries
	 * @see ResourceUsage
	 */
	public Collection<ResourceUsage> getPeakResourceUse(long startTime, long finishTime) {
		Collection<ProfileEntry> avail = profile.getAvailability(startTime, finishTime);
		ArrayList<ResourceUsage> usage = new ArrayList<>(avail.size());

		for (ProfileEntry e : avail) {
			usage.add(new ResourceUsage(e.getTime(), super.getCapacity() - e.getNumResources()));
		}
		return usage;
	}

	/**
	 * Computes the number of resource units available in a set of slots
	 * @param slots the slot set
	 * @return the number of units
	 */
	protected long getResourceUnits(Collection<TimeSlot> slots) {
		long units = 0;
		for (TimeSlot s : slots) {
			units += s.getDuration() * s.getNumResources();
		}
		return units;
	}

	/**
	 * Creates a String representation of this resource pool.
	 * @return the String representation.
	 */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("capacity", super.getCapacity())
				.add("profile", this.profile)
				.toString();
	}
}

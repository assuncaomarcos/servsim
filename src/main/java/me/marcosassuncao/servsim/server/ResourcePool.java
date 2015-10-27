package me.marcosassuncao.servsim.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import me.marcosassuncao.servsim.SimEntity;
import me.marcosassuncao.servsim.SimEvent;
import me.marcosassuncao.servsim.job.WorkUnit;
import me.marcosassuncao.servsim.profile.ProfileEntry;
import me.marcosassuncao.servsim.profile.RangeList;
import me.marcosassuncao.servsim.profile.SingleProfile;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * This class represents a pool of resources. This class implements
 * a delegation of {@link SingleProfile}.
 * 
 * @author Marcos Dias de Assuncao
 */
public abstract class ResourcePool extends SimEntity {
	private ArrayList<ResourceStatusListener> listeners;
	private int capacity;
	
	/**
	 * Creates a new resource pool
	 * @param capacity the number of resources to create in the list
	 */
	public ResourcePool(int capacity) {
		super("ResourcePool-" + UUID.randomUUID());
		checkArgument(capacity > 0, "Capacity must be >= 0");
		this.capacity = capacity;
	}
	
	/**
	 * Gets the resource capacity
	 * @return the resource capacity
	 */
	public int getCapacity() {
		return this.capacity;
	}
	
	/**
	 * Adds a resource status listener
	 * @param listener a resource status listener
	 * @return <code>true</code> if successful
	 */
	public boolean addResourceStatusListener(ResourceStatusListener listener) {
		return getStatusListeners().add(listener);
	}
	
	/**
	 * Removes a resource status listener
	 * @param listener a resource status listener
	 * @return <code>true</code> if successful
	 */
	public boolean removeResourceStatusListener(ResourceStatusListener listener) {
		return getStatusListeners().remove(listener);
	}
	
	/**
	 * Returns the list of registered resource status listeners
	 * @return a collection containing the listeners
	 */
	protected Collection<ResourceStatusListener> getStatusListeners() {
		if (listeners == null) {
			listeners = new ArrayList<>();
		}
		return listeners;
	}
	
	/**
	 * Returns a profile entry with resources available at a given time.
	 * @param readyTime the time from which availability is to be checked
	 * @return a {@link ProfileEntry} with ranges available at the given time.
	 */
	public abstract ProfileEntry checkAvailability(long readyTime);
	
	/**
	 * Returns a profile entry if a given job with the characteristics
	 * provided can be scheduled. 
	 * @param numRes the number of resources.
	 * @param startTime the start time of the job/reservation
	 * @param duration the duration of the job/reservation
	 * @return a {@link ProfileEntry} with the start time provided and the 
	 * ranges available at that time OR <tt>null</tt> if not enough resources are found.
	 */
	public abstract ProfileEntry checkAvailability(int numRes, long startTime, long duration);
	
	/**
	 * Returns a profile entry if a given job with the characteristics
	 * provided can be scheduled. 
	 * @param numRes the number of resources.
	 * @param startTime the start time of the job/reservation
	 * @param duration the duration of the job/reservation
	 * @param flexible defines whether less resources than originally requested is allowed
	 * @return a {@link ProfileEntry} with the start time provided and the 
	 * ranges available at that time OR <tt>null</tt> if not enough resources are found.
	 */
	public abstract ProfileEntry checkAvailability(int numRes, long startTime, 
			long duration, boolean flexible);
	
	/**
	 * Selects an entry able to provide enough resources to handle a job. The method 
	 * iterates the profile until it finds enough resources for the job, starting 
	 * from the current simulation time.
	 * @param numRes the number of resources
	 * @param duration the duration in seconds to execute the job
	 * @return an {@link ProfileEntry} with the time at which the job can start
	 * and the ranges available at that time.
	 */
	public abstract ProfileEntry findStartTime(int numRes, long duration);
	
	/**
	 * Allocates a list of resource ranges to a job/reservation.
	 * @param task the work unit for which resources need to be allocated
	 * @param resources the list of resource ranges selected
	 * @param startTime the start time of the job/reservation
	 */
	public abstract void allocateResources(WorkUnit task, RangeList resources, long startTime);
	
	/**
	 * Includes a time slot in this availability profile. This is useful if 
	 * your scheduling strategy cancels a job and you want to update the 
	 * availability profile.
	 * @param startTime the start time of the time slot.
	 * @param finishTime the finish time of the time slot.
	 * @param list the list of ranges of resources in the slot.
	 * @return <tt>true</tt> if the slot was included; <tt>false</tt> otherwise.
	 */
	public abstract boolean releaseResources(long startTime, long finishTime, RangeList list);
	
	/**
	 * Returns the resource utilisation during a given period
	 * @param startTime the initial time
	 * @param endTime the finish time
	 * @return the resource utilisation between <tt>0.0</tt> and <tt>1.0</tt>
	 */
	public abstract double getUtilization(long startTime, long endTime);
					
	@Override
	public void onStart() { }

	@Override
	public void process(SimEvent ev) { }

	@Override
	public void onShutdown() { }
	
	/**
	 * Possible resource statuses.
	 */

	public enum ResourceStatus {
		/** Resource is shutting down */
		SHUTTING_DOWN,
		
		/** Resource is off */
		OFF,
		
		/** Resource is booting */
		BOOTING,
		
		/** Resource is powered on */
		ON;
	}
}

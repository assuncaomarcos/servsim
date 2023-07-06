package me.marcosassuncao.servsim;

import me.marcosassuncao.servsim.event.AbstractEvent;

import com.google.common.base.MoreObjects;

/**
 * This class represents a simulation event that is sent by one entity to another.
 * 
 * @see Simulation
 * @see SimEntity

 * @author Marcos Dias de Assuncao
 */
@SuppressWarnings("rawtypes")
public class SimEvent extends AbstractEvent<Enum, Object>
		implements Cloneable, Comparable<SimEvent> {
	// These attributes are just to create a serial number for the 
	// event to maintain their temporal order when they are added to a queue.
	private static long nextSerial = 0;
	private final long serial;
	
	private final int srcEntity;			// the entity that created the event
	private final int dstEntity;			// the entity that will process the event
	
	/** A constant to represent the delay of an event that must be scheduled now */
	public static final long SEND_NOW = 0L;
	
	// ------ Package level methods ------
		   
	/**
	 * Creates a new event.
	 * @param type the type of event
	 * @param content the subject or content of the event
	 * @param time the simulation time at which the event should be handled
	 * @param srcEntity the source entity that created the event
	 * @param dstEntity the entity that should handle the event
	 */
	public SimEvent(Enum type, Object content, long time, int srcEntity, int dstEntity) {
		super(type, content, time);
		this.srcEntity = srcEntity;
		this.dstEntity = dstEntity;
		this.serial = createSerial();
	}
	
	/**
	 * Gets the id of the entity that triggered the event.
	 * @return the id of the entity that triggered the event.
	 */
	public int source() {
		return srcEntity;
	}
	
	/**
	 * Gets the id of the entity that will handle the event.
	 * @return the id of the entity that will handle the event.
	 */
	public int destination() {
		return dstEntity;
	}
	
	/**
	 * Gets the content of this event
	 * @return the content of this event
	 */
	public Object content() {
		return super.subject();
	}
	
	/**
	 * @see Comparable#compareTo(Object)
	 */
	public int compareTo(SimEvent event) {
		int comp = Long.compare(time(), event.time());
		if (comp == 0) {
			comp = Long.compare(serial, event.serial);
		}
		return comp;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
	       .add("time", super.time())
	       .add("type", super.type())
	       .add("srcEntity", srcEntity)
	       .add("targetEntity", dstEntity)
	       .add("content", super.subject())
	       .toString();
	}
	
	/**
	 * Returns a clone of this event
	 * @return a cloned event
	 */
	public Object clone() {
		return new SimEvent(super.type(), super.subject(), super.time(), srcEntity, dstEntity);
	}
		
	/* synchronised ID creation */
	private static synchronized long createSerial() {
		return ++nextSerial;
	}
	
	/**
	 * Basic types of simulation events.
	 */
	public enum Type {
		/** A task arrives in the system */
		TASK_ARRIVE,
		
		/** Used by certain scheduler to signal when a task must start */
		TASK_START,
		
		/** A task is completed by a resource */
		TASK_COMPLETE,
		
		/** A task is cancelled by a resource */
		TASK_CANCEL,
		
		/** A task is paused by a resource */
		TASK_PAUSE,
		
		/** Results are received by an entity */
		RESULT_ARRIVE,
		
		/** A new simulation entity arrives in the system */
		ENTITY_ARRIVE,
		
		/** A given simulation entity leaves the system */
		ENTITY_LEAVE,
		
		/** An internal event that the entity sent to itself */
		ENTITY_INTERNAL_EVENT,
		
		/** A resource reservation request arrived at a resource */
		RESERVATION_REQUEST,
		
		/** A resource reservation starts */
		RESERVATION_START,
		
		/** A resource reservation is completed */
		RESERVATION_COMPLETE,
		
		/** A resource reservation is cancelled by its requester */
		RESERVATION_CANCEL,
		
		/** Response to a reservation request */
		RESERVATION_RESPONSE
	}
}
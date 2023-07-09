package me.marcosassuncao.servsim;

import me.marcosassuncao.servsim.event.AbstractEvent;

import com.google.common.base.MoreObjects;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class represents a simulation event
 * that is sent by one entity to another.
 *
 * @see Simulation
 * @see SimEntity

 * @author Marcos Dias de Assuncao
 */
@SuppressWarnings("rawtypes")
public class SimEvent extends AbstractEvent<Enum, Object>
        implements Cloneable, Comparable<SimEvent> {
    /**
     * This is to create a serial number for the event to maintain
     * their temporal order when they are added to a queue.
     */
    private static final AtomicInteger SERIAL_COUNTER = new AtomicInteger(0);

    /**
     * Serial code to maintain time ordering of events in event queues.
     */
    private final long serial;

    /**
     * The entity that created the event.
     */
    private final int srcEntity;

    /**
     * The entity that will process the event.
     */
    private final int dstEntity;

    /** A constant to represent the delay of an event
     *  that must be scheduled now. */
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
    public SimEvent(final Enum type,
                    final Object content,
                    final long time,
                    final int srcEntity,
                    final int dstEntity) {
        super(type, content, time);
        this.srcEntity = srcEntity;
        this.dstEntity = dstEntity;
        this.serial = SERIAL_COUNTER.incrementAndGet();
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
     * Gets the content of this event.
     * @return the content of this event
     */
    public Object content() {
        return super.subject();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final SimEvent event) {
        int comp = Long.compare(time(), event.time());
        return comp == 0 ? Long.compare(serial, event.serial) : comp;
    }

    /**
     * Creates a String representation of this entity.
     * @return the String representation.
     */
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
     * Returns a clone of this event.
     * @return a cloned event
     */
    public Object clone() {
        return new SimEvent(super.type(),
                super.subject(), super.time(),
                srcEntity, dstEntity);
    }

    /**
     * Basic types of simulation events.
     */
    public enum Type {
        /** A task arrives in the system. */
        TASK_ARRIVE,

        /** Used by certain scheduler to signal when a task must start. */
        TASK_START,

        /** A task is completed by a resource. */
        TASK_COMPLETE,

        /** A task is cancelled by a resource. */
        TASK_CANCEL,

        /** A task is paused by a resource. */
        TASK_PAUSE,

        /** Results are received by an entity. */
        RESULT_ARRIVE,

        /** A new simulation entity arrives in the system. */
        ENTITY_ARRIVE,

        /** A given simulation entity leaves the system. */
        ENTITY_LEAVE,

        /** An internal event that the entity sent to itself. */
        ENTITY_INTERNAL_EVENT,

        /** A resource reservation request arrived at a resource. */
        RESERVATION_REQUEST,

        /** A resource reservation starts. */
        RESERVATION_START,

        /** A resource reservation is completed. */
        RESERVATION_COMPLETE,

        /** A resource reservation is cancelled by its requester. */
        RESERVATION_CANCEL,

        /** Response to a reservation request. */
        RESERVATION_RESPONSE
    }
}

package me.marcosassuncao.servsim.function;

import java.util.function.Predicate;

import me.marcosassuncao.servsim.SimEvent;

/**
 * Filter that selects the events sent by a given entity.
 *
 * @author Marcos Dias de Assuncao
 */

public class EventSentByEntityPredicate implements Predicate<SimEvent> {
    /**
     * The event whose ID matches this value, passes the predicated.
     */
    private final int entityId;

    /**
     * Creates a new filter.
     * @param entityId the id of the entity
     */
    public EventSentByEntityPredicate(final int entityId) {
        this.entityId = entityId;
    }

    /**
     * Gets the entity id associated with this filter.
     * @return the entity id associated with this filter
     */
    public int getEntityId() {
        return entityId;
    }

    /**
     * Tests a given event.
     * @param ev the event to be tested
     * @see Predicate#test(Object)
     * @return <code>true</code> if the event matches the predicate
     */
    public boolean test(final SimEvent ev) {
        return ev.source() == entityId;
    }
}

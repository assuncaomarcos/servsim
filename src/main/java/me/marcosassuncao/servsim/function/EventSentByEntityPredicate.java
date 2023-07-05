package me.marcosassuncao.servsim.function;

import java.util.function.Predicate;

import me.marcosassuncao.servsim.SimEvent;

/**
 * Filter that selects the events sent by a given entity.
 * 
 * @author Marcos Dias de Assuncao
 */

public class EventSentByEntityPredicate implements Predicate<SimEvent> {
	private final int entityId;
	
	/**
	 * Creates a new filter.
	 * @param entityId the id of the entity
	 */
	public EventSentByEntityPredicate(int entityId) {
		this.entityId = entityId;
	}
	
	/**
	 * Gets the entity id associated with this filter
	 * @return the entity id associated with this filter
	 */
	public int getEntityId() {
		return entityId;
	}

	/**
	 * @see Predicate#test(Object)
	 */
	public boolean test(SimEvent ev) {
		return ev.source() == entityId;
	}
}

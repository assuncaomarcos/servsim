package me.marcosassuncao.servsim.event;

import java.util.function.Predicate;

/**
 * Entity capable of filtering events.
 * Based on event framework from ONOS project.
 *
 * @author Marcos Dias de Assuncao
 *
 * @param <E> The event type to which this filter applies
 */
@SuppressWarnings(value = "rawtypes")
public interface EventFilter<E extends Event> extends Predicate<E> {

}

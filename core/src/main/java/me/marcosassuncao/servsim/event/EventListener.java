package me.marcosassuncao.servsim.event;

/**
 * Entity capable of receiving and handling events.
 *
 * @author Marcos Dias de Assuncao
 *
 * @param <E> The event type to which this filter applies
 */
@SuppressWarnings("rawtypes")
public interface EventListener<E extends Event> extends EventFilter<E> {

    /**
     * Reacts to an event.
     * @param event event to be handled
     */
    void event(E event);

}

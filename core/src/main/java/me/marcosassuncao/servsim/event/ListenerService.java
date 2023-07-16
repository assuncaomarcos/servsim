package me.marcosassuncao.servsim.event;

/**
 * Abstraction of a component capable of notifying listeners.
 *
 * @author Marcos Dias de Assuncao
 *
 * @param <E> an event type
 * @param <L> an event listener
 */
@SuppressWarnings("rawtypes")
public interface ListenerService<E extends Event, L extends EventListener<E>> {

    /**
     * Adds a listener.
     * @param listener listener to add
     */
    void addListener(L listener);

    /**
     * Removes the specified listener.
     * @param listener listener to remove
     */
    void removeListener(L listener);

}

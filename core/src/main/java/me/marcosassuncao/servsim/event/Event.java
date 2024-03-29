package me.marcosassuncao.servsim.event;

/**
 * Abstraction of an event.
 *
 * @author Marcos Dias de Assuncao
 *
 * @param <T> the event type
 * @param <S> the event subject
 */
@SuppressWarnings("rawtypes")
public interface Event<T extends Enum, S> {

    /**
     * Returns the time when the event occurred.
     * @return the simulation time
     */
    long time();

    /**
     * Returns the type of the event.
     * @return event type
     */
    T type();

    /**
     * Returns the subject of the event.
     * @return the subject of the event.
     */
    S subject();
}

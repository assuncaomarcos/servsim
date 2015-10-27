package me.marcosassuncao.servsim.event;

/**
 * Abstraction of sink capable of processing a given event type.
 * 
 * @author Marcos Dias de Assuncao
 *
 */
@SuppressWarnings("rawtypes")
public interface EventSink<E extends Event> {

    /**
     * Processes the specified event.
     * @param event event to be processed
     */
    void process(E event);
}

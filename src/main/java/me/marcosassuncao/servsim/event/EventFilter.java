package me.marcosassuncao.servsim.event;

import java.util.function.Predicate;

/**
 * Entity capable of filtering events. 
 * Based on event framework from ONOS project.
 * 
 * @author Marcos Dias de Assuncao
 */
@SuppressWarnings("rawtypes")
public interface EventFilter<E extends Event> extends Predicate<E> { 
	
}

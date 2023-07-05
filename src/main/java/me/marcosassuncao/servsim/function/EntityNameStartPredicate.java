package me.marcosassuncao.servsim.function;

import java.util.function.Predicate;

import me.marcosassuncao.servsim.SimEntity;
import me.marcosassuncao.servsim.Simulation;

/**
 * Implements a filter that filters entities whose name starts with a given string.
 * 
 * @see Simulation#getEntities(Predicate)
 * 
 * @author Marcos Dias de Assuncao
 */

public class EntityNameStartPredicate implements Predicate<SimEntity> {
	private final String nameStart;

	/**
	 * Creates a new filter.
	 * @param nameStart the string used for filtering
	 */
	public EntityNameStartPredicate(String nameStart) {
		this.nameStart = nameStart;
	}
	
	/**
	 * @see Predicate#test(Object)
	 */
	public boolean test(SimEntity entity) {
		return entity.getName().startsWith(nameStart);
	}
}

package me.marcosassuncao.servsim.function;

import java.util.function.Predicate;

import me.marcosassuncao.servsim.SimEntity;
import me.marcosassuncao.servsim.Simulation;

/**
 * Implements a filter that filters entities whose class 
 * matches what is provided.
 * 
 * @see Simulation#getEntities(EntityFilter)
 * 
 * @author Marcos Dias de Assuncao
 */

public class EntityClassPredicate implements Predicate<SimEntity> {
	private Class<? extends SimEntity> theClass;

	/**
	 * Creates a new filter.
	 * @param aClass the class used for matching
	 */
	public EntityClassPredicate(Class<? extends SimEntity> aClass) {
		this.theClass = aClass;
	}
	
	/**
	 * @see {@link EntityFilter#match(SimEntity)}
	 */
	public boolean test(SimEntity entity) {
		return theClass.isInstance(entity);
	}
}

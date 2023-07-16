package me.marcosassuncao.servsim.function;

import java.util.function.Predicate;

import me.marcosassuncao.servsim.SimEntity;
import me.marcosassuncao.servsim.Simulation;

/**
 * Implements a filter that filters entities whose class
 * matches what is provided.
 *
 * @see Simulation#getEntities(Predicate)
 *
 * @author Marcos Dias de Assuncao
 */

public class EntityClassPredicate implements Predicate<SimEntity> {
    /**
     * The class for which this predicate applies.
     */
    private final Class<? extends SimEntity> theClass;

    /**
     * Creates a new filter.
     * @param aClass the class used for matching
     */
    public EntityClassPredicate(final Class<? extends SimEntity> aClass) {
        this.theClass = aClass;
    }

    /**
     * @see Predicate#test(Object)
     *
     * @param entity the entity being tested
     * @return <code>true</code> if the entity if an instance
     * of the provided class
     */
    public boolean test(final SimEntity entity) {
        return theClass.isInstance(entity);
    }
}

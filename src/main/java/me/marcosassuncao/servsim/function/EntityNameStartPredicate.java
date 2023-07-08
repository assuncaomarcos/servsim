package me.marcosassuncao.servsim.function;

import java.util.function.Predicate;

import me.marcosassuncao.servsim.SimEntity;
import me.marcosassuncao.servsim.Simulation;

/**
 * Implements a filter that filters entities whose name starts
 * with a given string.
 *
 * @see Simulation#getEntities(Predicate)
 *
 * @author Marcos Dias de Assuncao
 */

public class EntityNameStartPredicate implements Predicate<SimEntity> {
    /**
     * Entities whose names start with this string, match the predicate.
     */
    private final String nameStart;

    /**
     * Creates a new filter.
     * @param nameStart the string used for filtering
     */
    public EntityNameStartPredicate(final String nameStart) {
        this.nameStart = nameStart;
    }

    /**
     * Tests a given entity.
     * @param entity the entity to be tested
     * @see Predicate#test(Object)
     * @return <code>true</code> if the entity matches the predicate
     */
    public boolean test(final SimEntity entity) {
        return entity.getName().startsWith(nameStart);
    }
}

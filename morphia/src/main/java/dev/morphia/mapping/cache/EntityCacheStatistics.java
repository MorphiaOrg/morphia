package dev.morphia.mapping.cache;


/**
 * This class stores various statistics on an EntityCache
 */
public class EntityCacheStatistics {
    private int entities;
    private int hits;
    private int misses;

    /**
     * Copies the statistics
     *
     * @return the copy
     */
    public EntityCacheStatistics copy() {
        final EntityCacheStatistics copy = new EntityCacheStatistics();
        copy.entities = entities;
        copy.hits = hits;
        copy.misses = misses;
        return copy;
    }

    /**
     * Increments the entity count
     */
    public void incEntities() {
        entities++;
    }

    /**
     * Increments the hit count
     */
    public void incHits() {
        hits++;
    }

    /**
     * Increments the miss count
     */
    public void incMisses() {
        misses++;
    }

    /**
     * Clears the statistics
     */
    public void reset() {
        entities = 0;
        hits = 0;
        misses = 0;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + entities + " entities, " + hits + " hits, " + misses + " misses.";
    }
}

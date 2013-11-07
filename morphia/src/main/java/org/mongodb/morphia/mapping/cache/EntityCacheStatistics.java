package org.mongodb.morphia.mapping.cache;


// note that it is not thread safe, currently it does not need to be.
public class EntityCacheStatistics {
    private int entities;
    private int hits;
    private int misses;

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + entities + " entities, " + hits + " hits, " + misses + " misses.";
    }

    public void reset() {
        entities = 0;
        hits = 0;
        misses = 0;
    }

    public EntityCacheStatistics copy() {
        final EntityCacheStatistics copy = new EntityCacheStatistics();
        copy.entities = entities;
        copy.hits = hits;
        copy.misses = misses;
        return copy;
    }

    public void incHits() {
        hits++;
    }

    public void incMisses() {
        misses++;
    }

    public void incEntities() {
        entities++;
    }
}

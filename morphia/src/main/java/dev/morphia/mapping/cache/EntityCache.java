package dev.morphia.mapping.cache;


import dev.morphia.Key;

/**
 * A primarily internal class used by MorphiaIterator to track entities loaded from mongo to prevent multiple loads of objects when keys
 * are
 * seen multiple times in a query result.
 */
public interface EntityCache {
    /**
     * Looks for a Key in the cache
     *
     * @param k the Key to search for
     * @return true if the Key is found
     */
    Boolean exists(Key<?> k);

    /**
     * Clears the cache
     */
    void flush();

    /**
     * Returns the entity for a Key
     *
     * @param k   the Key to search for
     * @param <T> the type of the entity
     * @return the entity
     */
    <T> T getEntity(Key<T> k);

    /**
     * Returns a proxy for the entity for a Key
     *
     * @param k   the Key to search for
     * @param <T> the type of the entity
     * @return the proxy
     */
    <T> T getProxy(Key<T> k);

    /**
     * Notifies the cache of the existence of a Key
     *
     * @param k      the Key
     * @param exists true if the Key represents an existing entity
     */
    void notifyExists(Key<?> k, boolean exists);

    /**
     * Adds an entity to the cache
     *
     * @param k   the Key of the entity
     * @param t   the entity
     * @param <T> the type of the entity
     */
    <T> void putEntity(Key<T> k, T t);

    /**
     * Adds a proxy to the cache
     *
     * @param k   the Key of the entity
     * @param t   the proxy
     * @param <T> the type of the entity
     */
    <T> void putProxy(Key<T> k, T t);

    /**
     * @return the stats for this cache
     */
    EntityCacheStatistics stats();
}

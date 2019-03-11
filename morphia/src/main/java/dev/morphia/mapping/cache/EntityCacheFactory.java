package dev.morphia.mapping.cache;

/**
 * Factory for entity caches.
 */
public interface EntityCacheFactory {

    /**
     * Called for every query. The cache is used during queries for by-id lookups.
     *
     * @return the cache
     */
    EntityCache createCache();
}

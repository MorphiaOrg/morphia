package org.mongodb.morphia.mapping.cache;

/**
 * Default implementation of cache factory, returning the default entity cache.
 */
public class DefaultEntityCacheFactory implements EntityCacheFactory {
    
    public EntityCache createCache() {
        return new DefaultEntityCache();
    }
}

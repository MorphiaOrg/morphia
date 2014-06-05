package org.mongodb.morphia.mapping.cache;


import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.lazy.LazyFeatureDependencies;
import org.mongodb.morphia.mapping.lazy.proxy.ProxyHelper;
import relocated.morphia.org.apache.commons.collections.ReferenceMap;

import java.util.HashMap;
import java.util.Map;


@SuppressWarnings({"rawtypes", "unchecked"})
public class DefaultEntityCache implements EntityCache {

    @SuppressWarnings({"rawtypes", "unchecked"})
    private final Map entityMap = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);
    private final Map proxyMap = new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.WEAK);
    private final Map<Key, Boolean> existenceMap = new HashMap<Key, Boolean>();
    private final EntityCacheStatistics stats = new EntityCacheStatistics();

    public Boolean exists(final Key<?> k) {
        if (entityMap.containsKey(k)) {
            stats.incHits();
            return true;
        }

        final Boolean b = existenceMap.get(k);
        if (b == null) {
            stats.incMisses();
        } else {
            stats.incHits();
        }
        return b;
    }

    public void notifyExists(final Key<?> k, final boolean exists) {
        final Boolean put = existenceMap.put(k, exists);
        if (put == null || !put) {
            stats.incEntities();
        }
    }

    public <T> T getEntity(final Key<T> k) {
        final Object o = entityMap.get(k);
        if (o == null) {
            if (LazyFeatureDependencies.testDependencyFullFilled()) {
                final Object proxy = proxyMap.get(k);
                if (proxy != null) {
                    ProxyHelper.isFetched(proxy);
                    stats.incHits();
                    return (T) ProxyHelper.unwrap(proxy);
                }
            }
            // System.out.println("miss entity " + k + ":" + this);
            stats.incMisses();
        } else {
            stats.incHits();
        }
        return (T) o;
    }

    public <T> T getProxy(final Key<T> k) {
        final Object o = proxyMap.get(k);
        if (o == null) {
            // System.out.println("miss proxy " + k);
            stats.incMisses();
        } else {
            stats.incHits();
        }
        return (T) o;
    }

    public <T> void putProxy(final Key<T> k, final T t) {
        proxyMap.put(k, t);
        stats.incEntities();

    }

    public <T> void putEntity(final Key<T> k, final T t) {
        notifyExists(k, true); // already registers a write
        entityMap.put(k, t);
    }

    public void flush() {
        entityMap.clear();
        existenceMap.clear();
        proxyMap.clear();
        stats.reset();
    }

    public EntityCacheStatistics stats() {
        return stats.copy();
    }

}

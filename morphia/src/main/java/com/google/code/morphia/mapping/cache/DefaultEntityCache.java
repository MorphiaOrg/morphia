package com.google.code.morphia.mapping.cache;


import java.util.HashMap;
import java.util.Map;

import com.google.code.morphia.Key;
import com.google.code.morphia.mapping.lazy.LazyFeatureDependencies;
import com.google.code.morphia.mapping.lazy.proxy.ProxyHelper;
import relocated.morphia.org.apache.commons.collections.ReferenceMap;


@SuppressWarnings({ "rawtypes", "unchecked" })
public class DefaultEntityCache implements EntityCache {

  private final Map<Key, Object>      entityMap    = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);
  private final Map<Key, Object>      proxyMap     = new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.WEAK);
  private final Map<Key, Boolean>     existenceMap = new HashMap<Key, Boolean>();
  private final EntityCacheStatistics stats        = new EntityCacheStatistics();

  public Boolean exists(final Key<?> k) {
    if (entityMap.containsKey(k)) {
      stats.hits++;
      return true;
    }

    final Boolean b = existenceMap.get(k);
    if (b == null) {
      stats.misses++;
    } else {
      stats.hits++;
    }
    return b;
  }

  public void notifyExists(final Key<?> k, final boolean exists) {
      final Boolean put = existenceMap.put(k, exists);
      if(put == null || !put) {
          stats.entities++;
      }
  }

  public <T> T getEntity(final Key<T> k) {
    final Object o = entityMap.get(k);
    if (o == null) {
      if (LazyFeatureDependencies.testDependencyFullFilled()) {
        final Object proxy = proxyMap.get(k);
        if (proxy != null) {
          ProxyHelper.isFetched(proxy);
          stats.hits++;
          return (T) ProxyHelper.unwrap(proxy);
        }
      }
      // System.out.println("miss entity " + k + ":" + this);
      stats.misses++;
    } else {
      stats.hits++;
    }
    return (T) o;
  }

  public <T> T getProxy(final Key<T> k) {
    final Object o = proxyMap.get(k);
    if (o == null) {
      // System.out.println("miss proxy " + k);
      stats.misses++;
    } else {
      stats.hits++;
    }
    return (T) o;
  }

  public <T> void putProxy(final Key<T> k, final T t) {
    proxyMap.put(k, t);
    stats.entities++;

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

package org.mongodb.morphia.mapping.cache;


import org.mongodb.morphia.Key;

/**
 * A primarily internal class used by MorphiaIterator to track entities loaded from mongo to prevent multiple loads of objects when keys
 * are seen multiple times in a query result.
 */
public interface EntityCache {
  Boolean exists(Key<?> k);

  void notifyExists(Key<?> k, boolean exists);

  <T> T getEntity(Key<T> k);

  <T> T getProxy(Key<T> k);

  <T> void putProxy(Key<T> k, T t);

  <T> void putEntity(Key<T> k, T t);

  void flush();

  EntityCacheStatistics stats();
}

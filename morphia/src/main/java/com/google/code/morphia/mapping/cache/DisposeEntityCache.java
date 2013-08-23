/**
 *
 */
package com.google.code.morphia.mapping.cache;

import com.google.code.morphia.Key;

/**
 * キャッシュしません
 * @author Shoichi <nisin.lib@gmail.com>
 *
 */
public class DisposeEntityCache implements EntityCache {
	private final EntityCacheStatistics stats = new EntityCacheStatistics();

	public Boolean exists(Key<?> k) {
		stats.misses++;
		return false;
	}

	public void notifyExists(Key<?> k, boolean exists) {
	}

	public <T> T getEntity(Key<T> k) {
		stats.misses++;
		return null;
	}

	public <T> T getProxy(Key<T> k) {
		stats.misses++;
		return null;
	}

	public <T> void putProxy(Key<T> k, T t) {}

	public <T> void putEntity(Key<T> k, T t) {}

	public void flush() {
		stats.reset();
	}

	public EntityCacheStatistics stats() {
		return stats.copy();
	}

}

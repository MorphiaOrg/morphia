package com.google.code.morphia.mapping.cache;

import java.util.HashMap;
import java.util.Map;

import com.google.code.morphia.Key;
import com.google.code.morphia.logging.MorphiaLogger;
import com.google.code.morphia.logging.MorphiaLoggerFactory;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class DefaultCache implements Cache {
	
	private static final MorphiaLogger log = MorphiaLoggerFactory.get(DefaultCache.class);
	
	private final Map<Key, Object> entityMap = new HashMap<Key, Object>();
	private final Map<Key, Object> proxyMap = new HashMap<Key, Object>();
	private final Map<Key, Boolean> existenceMap = new HashMap<Key, Boolean>();
	private final CacheStatistics stats = new CacheStatistics();
	
	public Boolean exists(Key<?> k) {
		return existenceMap.get(k);
	}
	
	public void notifyExists(Key<?> k, boolean exists) {
		existenceMap.put(k, exists);
		stats.writes++;
	}
	
	public <T> T getEntity(Key<T> k) {
		Object o = entityMap.get(k);
		if (o == null) {
			// TODO opt: maybe we see if we have a proxy for that, that was
			// already fetched...
			stats.misses++;
		} else {
			stats.hits++;
		}
		return (T) o;
	}
	
	public <T> T getProxy(Key<T> k) {
		Object o = proxyMap.get(k);
		if (o == null) {
			// TODO opt: maybe we see if we have a proxy for that, that was
			// already fetched...
			stats.misses++;
		} else {
			stats.hits++;
		}
		return (T) o;
	}
	
	public <T> void putProxy(Key<T> k, T t) {
		proxyMap.put(k, t);
		stats.writes++;
		
	}
	
	public <T> void putEntity(Key<T> k, T t) {
		notifyExists(k, true); // already registers a write
		entityMap.put(k, t);
	}
	
	public void flush() {
		entityMap.clear();
		existenceMap.clear();
		proxyMap.clear();
		stats.reset();
	}
	
	public CacheStatistics stats() {
		return stats.copy();
	}
	
}

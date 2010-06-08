/**
 * 
 */
package com.google.code.morphia.mapping.cache;

import java.util.HashMap;
import java.util.Map;

import com.google.code.morphia.Key;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public abstract class AbstractCache implements Cache {
	protected final Map<Key<?>, Object> cache = new HashMap<Key<?>, Object>();
	
	
	public void clearAll() {
		cache.clear();
	}
	
	public Object get(Key<?> key) {

		Object object = cache.get(key);
		return object;
	}
	
	public Object put(Key<?> key, Object entity) {
		Object ret = cache.put(key, entity);
		return ret;
	}
	
	public Object removeByKey(Key<?> key) {
		return cache.remove(key);
	}
	
	public int size() {
		return cache.size();
	}

}

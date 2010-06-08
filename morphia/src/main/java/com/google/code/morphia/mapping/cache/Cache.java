/**
 * 
 */
package com.google.code.morphia.mapping.cache;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public interface Cache {
	Object put(CacheKey key, Object entity);
	
	Object get(CacheKey key);
	
	Object removeByKey(CacheKey key);
	
	// Object remove(Object o);
	
	// void clearByClass(String clazzName);
	
	void clearAll();
	
	int size();

}

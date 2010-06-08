/**
 * 
 */
package com.google.code.morphia.mapping.cache;

import com.google.code.morphia.Key;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public interface Cache {
	Object put(Key<?> key, Object entity);
	
	Object get(Key<?> key);
	
	Object removeByKey(Key<?> key);
	
	// Object remove(Object o);
	
	// void clearByClass(String clazzName);
	
	void clearAll();
	
	int size();

}

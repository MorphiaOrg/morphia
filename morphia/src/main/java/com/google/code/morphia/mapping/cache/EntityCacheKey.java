/**
 * 
 */
package com.google.code.morphia.mapping.cache;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
@SuppressWarnings("unchecked")
public class EntityCacheKey extends CacheKey {
	
	public EntityCacheKey(Class clazz, String id) {
		super(clazz.getName(), id);
	}
	
}

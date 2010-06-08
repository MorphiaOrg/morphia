/**
 * 
 */
package com.google.code.morphia.mapping.cache;

import com.mongodb.DBRef;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class ProxyCacheKey extends CacheKey {
	
	public ProxyCacheKey(DBRef ref) {
		super(ref.getRef(), ref.getId());
	}
	
}

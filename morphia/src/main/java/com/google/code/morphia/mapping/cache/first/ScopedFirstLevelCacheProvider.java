/**
 * 
 */
package com.google.code.morphia.mapping.cache.first;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * 
 */
public class ScopedFirstLevelCacheProvider implements FirstLevelCacheProvider {
	
	final static ThreadLocal<FirstLevelEntityCache> entityCacheLocal = new ThreadLocal<FirstLevelEntityCache>();
	final static ThreadLocal<FirstLevelProxyCache> proxyCacheLocal = new ThreadLocal<FirstLevelProxyCache>();
	
	public FirstLevelEntityCache getEntityCache() {
		FirstLevelEntityCache c = entityCacheLocal.get();
		if (c == null) {
			c = new DefaultFirstLevelEntityCache();
			entityCacheLocal.set(c);
		}
		return c;
	}
	
	public FirstLevelProxyCache getProxyCache() {
		FirstLevelProxyCache c = proxyCacheLocal.get();
		if (c == null) {
			c = new DefaultFirstLevelProxyCache();
			proxyCacheLocal.set(c);
		}
		return c;
	}
	
	public void release() {
		FirstLevelEntityCache firstLevelEntityCache = entityCacheLocal.get();
		if (firstLevelEntityCache != null) {
			firstLevelEntityCache.clearAll();
			entityCacheLocal.set(null);
		}
		
		FirstLevelProxyCache firstLevelProxyCache = proxyCacheLocal.get();
		if (firstLevelProxyCache != null) {
			firstLevelProxyCache.clearAll();
			proxyCacheLocal.set(null);
		}
	}
}

/**
 * 
 */
package com.google.code.morphia;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * 
 */
public class VersionHelper {
	
	public static long nextValue(Long oldVersion) {
		long currentTimeMillis = System.currentTimeMillis();
		// very unlikely, but you never know
		if (oldVersion != null && oldVersion.longValue() == currentTimeMillis)
			currentTimeMillis++;
		return currentTimeMillis;
	}
	
}

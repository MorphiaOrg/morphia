/**
 * 
 */
package com.google.code.morphia.logging.slf4j;

import com.google.code.morphia.logging.Logr;
import com.google.code.morphia.logging.LogrFactory;

/**
 * @author doc
 */
public class SLF4JLoggerImplFactory implements LogrFactory {
	public Logr get(final Class<?> c) {
		return new SLF4JLogger(c);
	}
	
}

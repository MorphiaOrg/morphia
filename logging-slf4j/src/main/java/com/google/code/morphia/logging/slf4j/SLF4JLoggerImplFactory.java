/**
 * 
 */
package com.google.code.morphia.logging.slf4j;

import com.google.code.morphia.logging.LoggerImplFactory;
import com.google.code.morphia.logging.MorphiaLogger;

/**
 * @author doc
 */
public class SLF4JLoggerImplFactory implements LoggerImplFactory {
	public MorphiaLogger get(final Class c) {
		return new SLF4JLogger(c);
	}
	
}

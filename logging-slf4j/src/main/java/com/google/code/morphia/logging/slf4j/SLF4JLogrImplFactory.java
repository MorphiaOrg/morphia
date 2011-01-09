/**
 * 
 */
package com.google.code.morphia.logging.slf4j;

import com.google.code.morphia.logging.Logr;
import com.google.code.morphia.logging.LogrFactory;

/**
 * @author doc
 */
public class SLF4JLogrImplFactory implements LogrFactory {
	public Logr get(final Class<?> c) {
		return new SLF4JLogr(c);
	}
	
}

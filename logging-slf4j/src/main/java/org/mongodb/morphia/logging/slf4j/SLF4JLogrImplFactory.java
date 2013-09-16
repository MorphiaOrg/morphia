package org.mongodb.morphia.logging.slf4j;

import org.mongodb.morphia.logging.Logr;
import org.mongodb.morphia.logging.LogrFactory;

/**
 * @author doc
 */
public class SLF4JLogrImplFactory implements LogrFactory {
	public Logr get(final Class<?> c) {
		return new SLF4JLogr(c);
	}

}

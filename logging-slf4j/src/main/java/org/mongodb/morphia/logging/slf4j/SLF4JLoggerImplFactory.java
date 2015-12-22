package org.mongodb.morphia.logging.slf4j;

import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.LoggerFactory;

/**
 * Factory class for the sfl4j logger implementation.
 */
public class SLF4JLoggerImplFactory implements LoggerFactory {
    @Override
    public Logger get(final Class<?> c) {
        return new SLF4JLogger(c);
    }
}

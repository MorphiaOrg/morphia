package org.mongodb.morphia.logging.log4j;

import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.LoggerFactory;

/**
 * Factory class for the log4j logger implementation.
 */
public class Log4JLoggerImplFactory implements LoggerFactory {
    @Override
    public Logger get(final Class<?> c) {
        return new Log4JLogger(c);
    }

}

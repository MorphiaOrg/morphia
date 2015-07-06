package org.mongodb.morphia.logging.jdk;


import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.LoggerFactory;

/**
 * A logger factory using the JDK's logging.
 */
public class JDKLoggerFactory implements LoggerFactory {

    @Override
    public Logger get(final Class<?> c) {
        return new JDKLogger(c);
    }

}

package dev.morphia.logging.jdk;


import dev.morphia.logging.Logger;
import dev.morphia.logging.LoggerFactory;

/**
 * A logger factory using the JDK's logging.
 */
public class JDKLoggerFactory implements LoggerFactory {

    @Override
    public Logger get(final Class<?> c) {
        return new JDKLogger(c);
    }

}

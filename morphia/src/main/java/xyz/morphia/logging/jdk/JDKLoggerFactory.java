package xyz.morphia.logging.jdk;


import xyz.morphia.logging.Logger;
import xyz.morphia.logging.LoggerFactory;

/**
 * A logger factory using the JDK's logging.
 */
public class JDKLoggerFactory implements LoggerFactory {

    @Override
    public Logger get(final Class<?> c) {
        return new JDKLogger(c);
    }

}

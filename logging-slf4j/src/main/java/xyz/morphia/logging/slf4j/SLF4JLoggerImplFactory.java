package xyz.morphia.logging.slf4j;

import xyz.morphia.logging.Logger;
import xyz.morphia.logging.LoggerFactory;

/**
 * Factory class for the sfl4j logger implementation.
 */
public class SLF4JLoggerImplFactory implements LoggerFactory {
    @Override
    public Logger get(final Class<?> c) {
        return new SLF4JLogger(c);
    }
}

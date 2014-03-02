package org.mongodb.morphia.logging.slf4j;

import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.LoggerFactory;

public class SLF4JLoggerImplFactory implements LoggerFactory {
    public Logger get(final Class<?> c) {
        return new SLF4JLogger(c);
    }
}

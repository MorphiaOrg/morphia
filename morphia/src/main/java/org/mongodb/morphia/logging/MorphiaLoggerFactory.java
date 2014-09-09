package org.mongodb.morphia.logging;

import org.mongodb.morphia.logging.jdk.JDKLoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class MorphiaLoggerFactory {
    private static LoggerFactory loggerFactory;

    private static final List<String> FACTORIES = new ArrayList<String>();

    static {
        FACTORIES.add("org.mongodb.morphia.logging.slf4j.SLF4JLoggerImplFactory");
        FACTORIES.add(JDKLoggerFactory.class.getName());
    }

    private MorphiaLoggerFactory() {
    }
    
    private static synchronized void init() {
        if (loggerFactory == null) {
            chooseLoggerFactory();
        }
    }

    private static void chooseLoggerFactory() {
        for (final String f : FACTORIES) {
            loggerFactory = newInstance(f);
            if (loggerFactory != null) {
                loggerFactory.get(MorphiaLoggerFactory.class).info("LoggerImplFactory set to " + loggerFactory.getClass().getName());
                return;
            }
        }
        throw new IllegalStateException("Cannot instantiate any MorphiaLoggerFactory");
    }

    private static LoggerFactory newInstance(final String f) {
        try {
            return (LoggerFactory) Class.forName(f).newInstance();
        } catch (Throwable ignore) {
            return null;
        }
    }

    public static Logger get(final Class<?> c) {
        init();
        return loggerFactory.get(c);
    }

    /**
     * Register a LoggerFactory; last one registered is used. *
     */
    public static void registerLogger(final Class<? extends LoggerFactory> factoryClass) {
        if (loggerFactory == null) {
            FACTORIES.add(0, factoryClass.getName());
        } else {
            throw new IllegalStateException("LoggerImplFactory must be registered before logging is initialized.");
        }
    }

    public static void reset() {
        loggerFactory = null;
    }
}

package com.google.code.morphia.logging;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.code.morphia.logging.jdk.JDKLoggerFactory;


@SuppressWarnings({ "unchecked"})
public final class MorphiaLoggerFactory {
  private static LogrFactory loggerFactory;

  private static final List<String> factories = new ArrayList(Arrays.asList(JDKLoggerFactory.class.getName(),
    "com.google.code.morphia.logging.slf4j.SLF4JLogrImplFactory"));

  private static synchronized void init() {
    if (loggerFactory == null) {
      chooseLoggerFactory();
    }
  }

  private static void chooseLoggerFactory() {
    for (final String f : factories) {
      loggerFactory = newInstance(f);
      if (loggerFactory != null) {
        loggerFactory.get(MorphiaLoggerFactory.class).info("LoggerImplFactory set to " + loggerFactory.getClass().getName());
        return;
      }
    }
    throw new IllegalStateException("Cannot instantiate any MorphiaLoggerFactory");
  }

  private static LogrFactory newInstance(final String f) {
    try {
      final Class<?> c = Class.forName(f);
      return (LogrFactory) c.newInstance();
    } catch (Throwable ignore) {
      ignore.printStackTrace();
    }
    return null;
  }

  public static Logr get(final Class<?> c) {
    init();
    return loggerFactory.get(c);
  }

  /**
   * Register a LoggerFactory; last one registered is used. *
   */
  public static void registerLogger(final Class<? extends LogrFactory> factoryClass) {
    if (loggerFactory == null) {
      factories.add(0, factoryClass.getName());
    } else {
      throw new IllegalStateException("LoggerImplFactory must be registered before logging is initialized.");
    }
  }

  public static void reset() {
    loggerFactory = null;
  }
}

package com.google.code.morphia.logging;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.code.morphia.logging.jdk.JDKLoggerImplFactory;

public class MorphiaLoggerFactory {
	private static LoggerImplFactory loggerFactory = null;
	
	private static List<String> factories = Arrays.asList(JDKLoggerImplFactory.class.getName(),
	"com.google.code.morphia.logging.slf4j.SLF4JLoggerImplFactory");
	
	private static synchronized void init() {
		if (MorphiaLoggerFactory.loggerFactory == null) {
			chooseLoggerFactory();
		}
	}
	
	private static void chooseLoggerFactory() {
		Collections.reverse(MorphiaLoggerFactory.factories);
		for (String f : MorphiaLoggerFactory.factories) {
			MorphiaLoggerFactory.loggerFactory = newInstance(f);
            if (MorphiaLoggerFactory.loggerFactory != null) {
                loggerFactory.get(MorphiaLoggerFactory.class).info(
                        "LoggerImplFactory set to " + loggerFactory.getClass().getName());
                return;
            }
		}
		throw new IllegalStateException("Cannot instanciate any MorphiaLoggerFactory");
	}
	
	private static LoggerImplFactory newInstance(String f) {
		try {
			Class c = Class.forName(f);
			return (LoggerImplFactory) c.newInstance();
		} catch (Throwable ignore) {
		}
		return null;
	}
	
	public static final MorphiaLogger get(Class c) {
		init();
		return MorphiaLoggerFactory.loggerFactory.get(c);
	}
	
	public static void registerLoggerImplFactory(Class<? extends LoggerImplFactory> factoryClass) {
		if (MorphiaLoggerFactory.loggerFactory == null)
			MorphiaLoggerFactory.factories.add(factoryClass.getName());
		else
			throw new IllegalStateException("LoggerImplFactory must be registered before logging is initialized.");
	}
}

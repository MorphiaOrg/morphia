package com.google.code.morphia.logging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.code.morphia.logging.jdk.JDKLoggerFactory;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class MorphiaLogrFactory {
	private static LogrFactory loggerFactory = null;
	
	private static List<String> factories = new ArrayList(Arrays.asList(JDKLoggerFactory.class.getName(),
			"com.google.code.morphia.logging.slf4j.SLF4JLogrImplFactory"));
	
	private static synchronized void init() {
		if (MorphiaLogrFactory.loggerFactory == null) {
			chooseLoggerFactory();
		}
	}
	
	private static void chooseLoggerFactory() {
		Collections.reverse(MorphiaLogrFactory.factories);
		for (String f : MorphiaLogrFactory.factories) {
			MorphiaLogrFactory.loggerFactory = newInstance(f);
			if (MorphiaLogrFactory.loggerFactory != null) {
				loggerFactory.get(MorphiaLogrFactory.class).info(
						"LoggerImplFactory set to " + loggerFactory.getClass().getName());
				return;
			}
		}
		throw new IllegalStateException("Cannot instanciate any MorphiaLoggerFactory");
	}
	
	private static LogrFactory newInstance(String f) {
		try {
			Class<?> c = Class.forName(f);
			return (LogrFactory) c.newInstance();
		} catch (Throwable ignore) {
		}
		return null;
	}
	
	public static final Logr get(Class<?> c) {
		init();
		return MorphiaLogrFactory.loggerFactory.get(c);
	}
	
	/** Register a LoggerFactory; last one registered is used. **/
	public static void registerLogger(Class<? extends LogrFactory> factoryClass) {
		if (MorphiaLogrFactory.loggerFactory == null)
			MorphiaLogrFactory.factories.add(0, factoryClass.getName());
		else
			throw new IllegalStateException("LoggerImplFactory must be registered before logging is initialized.");
	}
}

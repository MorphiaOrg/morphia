package com.google.code.morphia.logging.jdk;

import com.google.code.morphia.logging.LoggerImplFactory;
import com.google.code.morphia.logging.MorphiaLogger;

public class JDKLoggerImplFactory implements LoggerImplFactory {
	
	public MorphiaLogger get(Class c) {
		return new JDKLogger(c);
	}
	
}

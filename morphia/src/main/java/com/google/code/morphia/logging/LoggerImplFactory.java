package com.google.code.morphia.logging;


public interface LoggerImplFactory {
	MorphiaLogger get(Class c);
}

package com.google.code.morphia.logging.jdk;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.code.morphia.logging.MorphiaLogger;

public class JDKLogger implements MorphiaLogger {
	
	private final Logger logger;
	
	public JDKLogger(Class c) {
		logger = Logger.getLogger(c.getName());
	}
	
	public boolean isTraceEnabled() {
		return logger.isLoggable(Level.FINER);
	}
	
	public void trace(String msg) {
		logger.log(Level.FINER, msg);
	}
	
	public void trace(String format, Object... arg) {
		logger.log(Level.FINER, format, arg);
	}
	
	public void trace(String msg, Throwable t) {
		logger.log(Level.FINER, msg, t);
	}
	
	public boolean isDebugEnabled() {
		return logger.isLoggable(Level.FINE);
	}
	
	public void debug(String msg) {
		logger.log(Level.FINE, msg);
	}
	
	public void debug(String format, Object... arg) {
		logger.log(Level.FINE, format, arg);
	}
	
	public void debug(String msg, Throwable t) {
		logger.log(Level.FINE, msg, t);
		
	}
	
	public boolean isInfoEnabled() {
		return logger.isLoggable(Level.INFO);
	}
	
	public void info(String msg) {
		logger.log(Level.INFO, msg);
	}
	
	public void info(String format, Object... arg) {
		logger.log(Level.INFO, format, arg);
	}
	
	public void info(String msg, Throwable t) {
		logger.log(Level.INFO, msg, t);
	}
	
	public boolean isWarningEnabled() {
		return logger.isLoggable(Level.WARNING);
	}
	
	public void warning(String msg) {
		logger.log(Level.WARNING, msg);
		
	}
	
	public void warning(String format, Object... arg) {
		logger.log(Level.WARNING, format, arg);
	}
	
	public void warning(String msg, Throwable t) {
		logger.log(Level.WARNING, msg, t);
	}
	
	public boolean isErrorEnabled() {
		return logger.isLoggable(Level.SEVERE);
	}
	
	public void error(String msg) {
		logger.log(Level.SEVERE, msg);
		
	}
	
	public void error(String format, Object... arg) {
		logger.log(Level.SEVERE, format, arg);
		
	}
	
	public void error(String msg, Throwable t) {
		logger.log(Level.SEVERE, msg, t);
	}
	
}

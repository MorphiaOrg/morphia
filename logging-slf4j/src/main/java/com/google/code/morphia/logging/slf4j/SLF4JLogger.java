package com.google.code.morphia.logging.slf4j;

import org.slf4j.Logger;

import com.google.code.morphia.logging.Logr;

public class SLF4JLogger implements Logr {
	private static final long serialVersionUID = 1L;
	private final Logger logger;
	
	public SLF4JLogger(final Class<?> c) {
		this.logger = org.slf4j.LoggerFactory.getLogger(c);
	}
	
	public boolean isTraceEnabled() {
		return this.logger.isTraceEnabled();
	}
	
	public void trace(final String msg) {
		this.logger.trace(msg);
	}
	
	public void trace(final String format, final Object... argArray) {
		this.logger.trace(format, argArray);
	}
	
	public void trace(final String msg, final Throwable t) {
		this.logger.trace(msg, t);
	}
	
	public boolean isDebugEnabled() {
		return this.logger.isDebugEnabled();
	}
	
	public void debug(final String msg) {
		this.logger.debug(msg);
	}
	
	public void debug(final String format, final Object... argArray) {
		this.logger.debug(format, argArray);
	}
	
	public void debug(final String msg, final Throwable t) {
		this.logger.debug(msg, t);
	}
	
	public boolean isInfoEnabled() {
		return this.logger.isInfoEnabled();
	}
	
	public void info(final String msg) {
		this.logger.info(msg);
	}
	
	public void info(final String format, final Object... argArray) {
		this.logger.info(format, argArray);
	}
	
	public void info(final String msg, final Throwable t) {
		this.logger.info(msg, t);
	}
	
	public boolean isWarningEnabled() {
		return this.logger.isWarnEnabled();
	}
	
	public void warning(final String msg) {
		this.logger.warn(msg);
	}
	
	public void warning(final String format, final Object... argArray) {
		this.logger.warn(format, argArray);
	}
	
	public void warning(final String msg, final Throwable t) {
		this.logger.warn(msg, t);
	}
	
	public boolean isErrorEnabled() {
		return this.logger.isErrorEnabled();
	}
	
	public void error(final String msg) {
		this.logger.error(msg);
	}
	
	public void error(final String format, final Object... argArray) {
		this.logger.error(format, argArray);
	}
	
	public void error(final String msg, final Throwable t) {
		this.logger.error(msg, t);
	}
	
}

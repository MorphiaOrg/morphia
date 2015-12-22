package org.mongodb.morphia.logging.slf4j;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class hands off log messages to the underlying slf4j logging system.
 */
public class SLF4JLogger implements org.mongodb.morphia.logging.Logger {
    private final Logger logger;

    /**
     * Create an SLF4JLogger with the given class name as its namespace.
     *
     * @param c the Class to use
     */
    public SLF4JLogger(final Class<?> c) {
        this.logger = LoggerFactory.getLogger(c);
    }

    @Override
    public boolean isTraceEnabled() {
        return this.logger.isTraceEnabled();
    }

    @Override
    public void trace(final String msg) {
        this.logger.trace(msg);
    }

    @Override
    public void trace(final String format, final Object... argArray) {
        this.logger.trace(format, argArray);
    }

    @Override
    public void trace(final String msg, final Throwable t) {
        this.logger.trace(msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return this.logger.isDebugEnabled();
    }

    @Override
    public void debug(final String msg) {
        this.logger.debug(msg);
    }

    @Override
    public void debug(final String format, final Object... argArray) {
        this.logger.debug(format, argArray);
    }

    @Override
    public void debug(final String msg, final Throwable t) {
        this.logger.debug(msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return this.logger.isInfoEnabled();
    }

    @Override
    public void info(final String msg) {
        this.logger.info(msg);
    }

    @Override
    public void info(final String format, final Object... argArray) {
        this.logger.info(format, argArray);
    }

    @Override
    public void info(final String msg, final Throwable t) {
        this.logger.info(msg, t);
    }

    @Override
    public boolean isWarningEnabled() {
        return this.logger.isWarnEnabled();
    }

    @Override
    public void warning(final String msg) {
        this.logger.warn(msg);
    }

    @Override
    public void warning(final String format, final Object... argArray) {
        this.logger.warn(format, argArray);
    }

    @Override
    public void warning(final String msg, final Throwable t) {
        this.logger.warn(msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return this.logger.isErrorEnabled();
    }

    @Override
    public void error(final String msg) {
        this.logger.error(msg);
    }

    @Override
    public void error(final String format, final Object... argArray) {
        this.logger.error(format, argArray);
    }

    @Override
    public void error(final String msg, final Throwable t) {
        this.logger.error(msg, t);
    }

}

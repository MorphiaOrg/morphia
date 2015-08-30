package org.mongodb.morphia.logging.log4j;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class hands off log messages to the underlying log4j logging system.
 */
public class Log4JLogger implements org.mongodb.morphia.logging.Logger {
    private final Logger logger;

    /**
     * Create an Log4JLogger with the given class name as its namespace.
     *
     * @param c the Class to use
     */
    public Log4JLogger(final Class<?> c) {
        logger = LogManager.getLogger(c);
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(final String msg) {
        logger.trace(msg);
    }

    @Override
    public void trace(final String format, final Object... arg) {
        logger.trace(format, arg);
    }

    @Override
    public void trace(final String msg, final Throwable t) {
        logger.trace(msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(final String msg) {
        logger.debug(msg);
    }

    @Override
    public void debug(final String format, final Object... arg) {
        logger.debug(format, arg);
    }

    @Override
    public void debug(final String msg, final Throwable t) {
        logger.debug(msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(final String msg) {
        logger.info(msg);
    }

    @Override
    public void info(final String format, final Object... arg) {
        logger.info(format, arg);
    }

    @Override
    public void info(final String msg, final Throwable t) {
        logger.info(msg, t);
    }

    @Override
    public boolean isWarningEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warning(final String msg) {
        logger.warn(msg);
    }

    @Override
    public void warning(final String format, final Object... arg) {
        logger.warn(format, arg);
    }

    @Override
    public void warning(final String msg, final Throwable t) {
        logger.warn(msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(final String msg) {
        logger.error(msg);
    }

    @Override
    public void error(final String format, final Object... arg) {
        logger.error(format, arg);
    }

    @Override
    public void error(final String msg, final Throwable t) {
        logger.error(msg, t);
    }

}

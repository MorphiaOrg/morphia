package dev.morphia.logging.jdk;

import java.util.logging.Level;
import java.util.logging.Logger;

public class FastestJDKLogger implements dev.morphia.logging.Logger {
    private final Logger logger;
    private final transient String className;

    public FastestJDKLogger(final Class c) {
        className = c.getName();
        logger = Logger.getLogger(className);
    }

    public void debug(final String msg) {
        log(Level.FINE, msg);
    }

    public void debug(final String format, final Object... arg) {
        log(Level.FINE, format, arg);
    }

    public void debug(final String msg, final Throwable t) {
        log(Level.FINE, msg, t);

    }

    public void error(final String msg) {
        log(Level.SEVERE, msg);

    }

    public void error(final String format, final Object... arg) {
        log(Level.SEVERE, format, arg);

    }

    public void error(final String msg, final Throwable t) {
        log(Level.SEVERE, msg, t);
    }

    public void info(final String msg) {
        log(Level.INFO, msg);
    }

    public void info(final String format, final Object... arg) {
        log(Level.INFO, format, arg);
    }

    public void info(final String msg, final Throwable t) {
        log(Level.INFO, msg, t);
    }

    public boolean isDebugEnabled() {
        return logger.isLoggable(Level.FINE);
    }

    public boolean isErrorEnabled() {
        return logger.isLoggable(Level.SEVERE);
    }

    public boolean isInfoEnabled() {
        return logger.isLoggable(Level.INFO);
    }

    public boolean isTraceEnabled() {
        return logger.isLoggable(Level.FINER);
    }

    public boolean isWarningEnabled() {
        return logger.isLoggable(Level.WARNING);
    }

    public void trace(final String msg) {
        log(Level.FINER, msg);
    }

    public void trace(final String format, final Object... arg) {
        log(Level.FINER, format, arg);
    }

    public void trace(final String msg, final Throwable t) {
        log(Level.FINER, msg, t);
    }

    public void warning(final String msg) {
        log(Level.WARNING, msg);
    }

    public void warning(final String format, final Object... arg) {
        log(Level.WARNING, format, arg);
    }

    public void warning(final String msg, final Throwable t) {
        log(Level.WARNING, msg, t);
    }

    public String getClassName() {
        return className;
    }

    public Logger getLogger() {
        return logger;
    }

    protected void log(final Level l, final String m, final Throwable t) {
        if (logger.isLoggable(l)) {
            logger.logp(l, className, null, m, t);
        }
    }

    protected void log(final Level l, final String f, final Object... a) {
        if (logger.isLoggable(l)) {
            logger.logp(l, className, null, f, a);
        }
    }

}

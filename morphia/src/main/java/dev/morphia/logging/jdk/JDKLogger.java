package dev.morphia.logging.jdk;


import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a logger using the JDK logging facilities.
 */
public class JDKLogger implements dev.morphia.logging.Logger {
    private final transient Logger logger;

    /**
     * Creates a logger.
     *
     * @param c the class to use for naming this logger.
     */
    public JDKLogger(final Class c) {
        logger = Logger.getLogger(c.getName());
    }

    @Override
    public void debug(final String msg) {
        log(Level.FINE, msg);
    }

    @Override
    public void debug(final String format, final Object... arg) {
        log(Level.FINE, format, arg);
    }

    @Override
    public void debug(final String msg, final Throwable t) {
        log(Level.FINE, msg, t);

    }

    @Override
    public void error(final String msg) {
        log(Level.SEVERE, msg);

    }

    @Override
    public void error(final String format, final Object... arg) {
        log(Level.SEVERE, format, arg);

    }

    @Override
    public void error(final String msg, final Throwable t) {
        log(Level.SEVERE, msg, t);
    }

    @Override
    public void info(final String msg) {
        log(Level.INFO, msg);
    }

    @Override
    public void info(final String format, final Object... arg) {
        log(Level.INFO, format, arg);
    }

    @Override
    public void info(final String msg, final Throwable t) {
        log(Level.INFO, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isLoggable(Level.FINE);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isLoggable(Level.SEVERE);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isLoggable(Level.INFO);
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isLoggable(Level.FINER);
    }

    @Override
    public boolean isWarningEnabled() {
        return logger.isLoggable(Level.WARNING);
    }

    @Override
    public void trace(final String msg) {
        log(Level.FINER, msg);
    }

    @Override
    public void trace(final String format, final Object... arg) {
        log(Level.FINER, format, arg);
    }

    @Override
    public void trace(final String msg, final Throwable t) {
        log(Level.FINER, msg, t);
    }

    @Override
    public void warning(final String msg) {
        log(Level.WARNING, msg);
    }

    @Override
    public void warning(final String format, final Object... arg) {
        log(Level.WARNING, format, arg);
    }

    @Override
    public void warning(final String msg, final Throwable t) {
        log(Level.WARNING, msg, t);
    }

    /**
     * returns an array (class, method) of the caller before our logger class in the stack
     */
    protected String[] getCaller() {
        final StackTraceElement[] stack = (new Throwable()).getStackTrace();
        final String loggerClassname = getClass().getName();

        final String callerName;
        final String callerMethod;

        int i = 0;
        while (i < stack.length) {
            final StackTraceElement ste = stack[i];
            final String fc = ste.getClassName();
            if (fc.equals(loggerClassname)) {
                break;
            }

            i++;
        }

        //skip an extra frame... we call ourselves
        i++;

        while (i < stack.length) {
            final StackTraceElement ste = stack[i];
            final String fc = ste.getClassName();
            if (!fc.equals(loggerClassname)) {
                callerMethod = ste.getMethodName();
                callerName = fc;
                return new String[]{callerName, callerMethod};
            }
            i++;
        }
        return new String[]{"", ""};
    }

    protected void log(final Level l, final String f, final Object... a) {
        if (logger.isLoggable(l)) {
            final String[] callerInfo = getCaller();
            logger.logp(l, callerInfo[0], callerInfo[1], f, a);
        }
    }

    protected void log(final Level l, final String m, final Throwable t) {
        if (logger.isLoggable(l)) {
            final String[] callerInfo = getCaller();
            logger.logp(l, callerInfo[0], callerInfo[1], m, t);
        }
    }
}

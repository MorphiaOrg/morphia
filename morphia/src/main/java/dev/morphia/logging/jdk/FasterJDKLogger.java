package dev.morphia.logging.jdk;

import java.util.logging.Level;

/**
 * Provides a faster logger.
 */
public class FasterJDKLogger extends FastestJDKLogger {

    /**
     * Creates a logger.
     *
     * @param c the class to use for naming this logger.
     */
    public FasterJDKLogger(final Class c) {
        super(c);
    }

    @Override
    protected void log(final Level l, final String m, final Throwable t) {
        if (getLogger().isLoggable(l)) {
            getLogger().logp(l, getClassName(), getCallingMethod(), m, t);
        }
    }

    @Override
    protected void log(final Level l, final String f, final Object... a) {
        if (getLogger().isLoggable(l)) {
            getLogger().logp(l, getClassName(), getCallingMethod(), f, a);
        }
    }

    private String getCallingMethod() {
        final StackTraceElement[] stack = (new Throwable()).getStackTrace();
        for (final StackTraceElement ste : stack) {
            if (getClassName().equals(ste.getClassName())) {
                return ste.getMethodName();
            }
        }

        return "<method name unknown due to misused non-private logger>";
    }
}

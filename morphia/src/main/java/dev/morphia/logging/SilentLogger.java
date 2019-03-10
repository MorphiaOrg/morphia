package dev.morphia.logging;


/**
 * Silent logger; it doesn't do anything!
 */
public class SilentLogger implements Logger {
    @Override
    public void debug(final String msg) {
    }

    @Override
    public void debug(final String format, final Object... arg) {
    }

    @Override
    public void debug(final String msg, final Throwable t) {
    }

    @Override
    public void error(final String msg) {
    }

    @Override
    public void error(final String format, final Object... arg) {
    }

    @Override
    public void error(final String msg, final Throwable t) {
    }

    @Override
    public void info(final String msg) {
    }

    @Override
    public void info(final String format, final Object... arg) {
    }

    @Override
    public void info(final String msg, final Throwable t) {
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public boolean isErrorEnabled() {
        return false;
    }

    @Override
    public boolean isInfoEnabled() {
        return false;
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public boolean isWarningEnabled() {
        return false;
    }

    @Override
    public void trace(final String msg) {
    }

    @Override
    public void trace(final String format, final Object... arg) {
    }

    @Override
    public void trace(final String msg, final Throwable t) {
    }

    @Override
    public void warning(final String msg) {
    }

    @Override
    public void warning(final String format, final Object... arg) {
    }

    @Override
    public void warning(final String msg, final Throwable t) {
    }
}

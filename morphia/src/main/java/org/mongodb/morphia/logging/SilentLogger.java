package org.mongodb.morphia.logging;


/**
 * Silent logger; it doesn't do anything!
 */
public class SilentLogger implements Logger {
  public boolean isTraceEnabled() {
    return false;
  }

  public void trace(final String msg) {
  }

  public void trace(final String format, final Object... arg) {
  }

  public void trace(final String msg, final Throwable t) {
  }

  public boolean isDebugEnabled() {
    return false;
  }

  public void debug(final String msg) {
  }

  public void debug(final String format, final Object... arg) {
  }

  public void debug(final String msg, final Throwable t) {
  }

  public boolean isInfoEnabled() {
    return false;
  }

  public void info(final String msg) {
  }

  public void info(final String format, final Object... arg) {
  }

  public void info(final String msg, final Throwable t) {
  }

  public boolean isWarningEnabled() {
    return false;
  }

  public void warning(final String msg) {
  }

  public void warning(final String format, final Object... arg) {
  }

  public void warning(final String msg, final Throwable t) {
  }

  public boolean isErrorEnabled() {
    return false;
  }

  public void error(final String msg) {
  }

  public void error(final String format, final Object... arg) {
  }

  public void error(final String msg, final Throwable t) {
  }
}

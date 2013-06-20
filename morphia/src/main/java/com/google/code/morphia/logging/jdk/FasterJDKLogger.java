package com.google.code.morphia.logging.jdk;


import java.util.logging.Level;


@SuppressWarnings("rawtypes")
public class FasterJDKLogger extends FastestJDKLogger {
  private static final long serialVersionUID = 1L;

  public FasterJDKLogger(final Class c) {
    super(c);
  }

  private String getCallingMethod() {
    final StackTraceElement[] stack = (new Throwable()).getStackTrace();
    for (final StackTraceElement ste : stack) {
      if (className.equals(ste.getClassName())) {
        return ste.getMethodName();
      }
    }

    return "<method name unknown due to misused non-private logger>";
  }

  @Override
  protected void log(final Level l, final String m, final Throwable t) {
    if (logger.isLoggable(l)) {
      logger.logp(l, className, getCallingMethod(), m, t);
    }
  }

  @Override
  protected void log(final Level l, final String f, final Object... a) {
    if (logger.isLoggable(l)) {
      logger.logp(l, className, getCallingMethod(), f, a);
    }
  }
}

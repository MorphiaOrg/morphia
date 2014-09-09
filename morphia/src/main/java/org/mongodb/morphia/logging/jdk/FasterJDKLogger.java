package org.mongodb.morphia.logging.jdk;

import java.util.logging.Level;

public class FasterJDKLogger extends FastestJDKLogger {
  private static final long serialVersionUID = 1L;

  public FasterJDKLogger(final Class c) {
    super(c);
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
}

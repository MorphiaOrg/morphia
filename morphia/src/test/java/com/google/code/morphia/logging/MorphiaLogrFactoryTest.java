package com.google.code.morphia.logging;


import org.junit.Test;
import com.google.code.morphia.TestBase;
import junit.framework.Assert;


/**
 * @author us@thomas-daily.de
 */
public class MorphiaLogrFactoryTest extends TestBase {

  static {

    MorphiaLoggerFactory.reset();
    MorphiaLoggerFactory.registerLogger(TestLoggerFactory.class);
  }

  @Test
  public void testChoice() throws Exception {
    final Logr logr = MorphiaLoggerFactory.get(Object.class);
    final String className = logr.getClass().getName();
    Assert.assertTrue(className.startsWith(TestLoggerFactory.class.getName() + "$"));
  }

  @Override
  public void tearDown() {
    MorphiaLoggerFactory.reset();
    super.tearDown();
  }

  static class TestLoggerFactory implements LogrFactory {
    public Logr get(final Class<?> c) {
      return new Logr() {

        public void warning(final String msg, final Throwable t) {

        }

        public void warning(final String format, final Object... arg) {

        }

        public void warning(final String msg) {

        }

        public void trace(final String msg, final Throwable t) {

        }

        public void trace(final String format, final Object... arg) {

        }

        public void trace(final String msg) {

        }

        public boolean isWarningEnabled() {

          return false;
        }

        public boolean isTraceEnabled() {

          return false;
        }

        public boolean isInfoEnabled() {

          return false;
        }

        public boolean isErrorEnabled() {

          return false;
        }

        public boolean isDebugEnabled() {

          return false;
        }

        public void info(final String msg, final Throwable t) {

        }

        public void info(final String format, final Object... arg) {

        }

        public void info(final String msg) {

        }

        public void error(final String msg, final Throwable t) {

        }

        public void error(final String format, final Object... arg) {

        }

        public void error(final String msg) {

        }

        public void debug(final String msg, final Throwable t) {

        }

        public void debug(final String format, final Object... arg) {

        }

        public void debug(final String msg) {

        }
      };
    }

  }
}

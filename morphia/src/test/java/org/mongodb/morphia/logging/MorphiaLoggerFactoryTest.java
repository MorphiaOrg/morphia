package org.mongodb.morphia.logging;

import org.junit.Test;
import org.mongodb.morphia.TestBase;

import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;

/**
 * @author us@thomas-daily.de
 */
public class MorphiaLoggerFactoryTest extends TestBase {

    @Test
    public void testOverrideLoggerWithCustomOne() throws Exception {
        // given
        MorphiaLoggerFactory.reset();
        MorphiaLoggerFactory.registerLogger(TestLoggerFactory.class);

        // when
        final Logger logger = MorphiaLoggerFactory.get(Object.class);

        // then
        assertThat((TestLogger) logger, isA(TestLogger.class));
    }

    @Override
    public void tearDown() {
        MorphiaLoggerFactory.reset();
        super.tearDown();
    }

    static class TestLoggerFactory implements LoggerFactory {
        public Logger get(final Class<?> c) {
            return new TestLogger();
        }
    }

    private static class TestLogger implements Logger {

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
    }
}

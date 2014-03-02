package org.mongodb.morphia.logging.slf4j;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * @author us@thomas-daily.de
 */
public class SLF4JLoggerTest extends TestBase {

    private ByteArrayOutputStream baos;
    private PrintStream oldErr;

    @Before
    @Override
    public void setUp() {
        oldErr = System.err;
        baos = new ByteArrayOutputStream();
        System.setErr(new PrintStream(baos));

        MorphiaLoggerFactory.registerLogger(SLF4JLoggerImplFactory.class);

        super.setUp();
    }

    @After
    public void tearDown() {
        System.setErr(oldErr);
    }

    @Test
    public final void testWarningString() {

        getDs().save(new LoggingTestEntity());
        // string type where int expected
        getDs().createQuery(LoggingTestEntity.class).field("i").equal("5");
        //CHECKSTYLE:OFF
        System.err.flush();
        //CHECKSTYLE:ON

        final String log = new String(baos.toByteArray());

        Assert.assertTrue(log.contains("WARN"));
        Assert.assertTrue(log.contains("instance of"));
        Assert.assertTrue(log.contains("java.lang.String"));
        Assert.assertTrue(log.contains("which is declared"));
        Assert.assertTrue(log.contains("LoggerImplFactory set to org.mongodb.morphia.logging.slf4j.SLF4JLoggerImplFactory"));
    }

}

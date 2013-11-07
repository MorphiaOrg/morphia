package org.mongodb.morphia.logging.slf4j;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * @author us@thomas-daily.de
 */
public class TestSLF4JLogrTest extends TestBase {
    public static class E {
        @Id
        private ObjectId id;

        private int i = 5;
    }

    private ByteArrayOutputStream baos;
    private PrintStream oldErr;

    @Before
    @Override
    public void setUp() {
        oldErr = System.err;
        baos = new ByteArrayOutputStream();
        System.setErr(new PrintStream(baos));

        MorphiaLoggerFactory.registerLogger(SLF4JLogrImplFactory.class);

        super.setUp();
    }

    @After
    public void tearDown() {
        System.setErr(oldErr);
    }

    @Test
    public final void testWarningString() {

        getDs().save(new E());
        // string type where int expected
        getDs().createQuery(E.class).field("i").equal("5");
        //CHECKSTYLE:OFF
        System.err.flush();
        //CHECKSTYLE:ON

        final String log = new String(baos.toByteArray());

        Assert.assertTrue(log.contains("WARN"));
        Assert.assertTrue(log.contains("instance of"));
        Assert.assertTrue(log.contains("java.lang.String"));
        Assert.assertTrue(log.contains("which is declared"));
        Assert.assertTrue(log.contains("LoggerImplFactory set to org.mongodb.morphia.logging.slf4j.SLF4JLogrImplFactory"));
    }

}

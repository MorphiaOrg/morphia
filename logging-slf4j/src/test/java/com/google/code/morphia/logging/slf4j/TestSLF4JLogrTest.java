/**
 * 
 */
package com.google.code.morphia.logging.slf4j;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import junit.framework.Assert;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.code.morphia.Key;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.logging.MorphiaLoggerFactory;

/**
 * @author us@thomas-daily.de
 */
public class TestSLF4JLogrTest extends TestBase
{
    public static class E
    {
        @Id
        ObjectId id;

        int i = 5;
    }

    private ByteArrayOutputStream baos;
    private PrintStream oldErr;

    @Before
    @Override
    public void setUp()
    {
        this.oldErr = System.err;
        this.baos = new ByteArrayOutputStream();
        System.setErr(new PrintStream(this.baos));

        MorphiaLoggerFactory.registerLogger(SLF4JLogrImplFactory.class);

        super.setUp();
    }

    @After
    public void tearDown()
    {
        System.setErr(this.oldErr);
    }

    @Test
    public final void testWarningString()
    {

        final Key<E> e = this.ds.save(new E());
        // string type where int expected
        this.ds.createQuery(E.class).field("i").equal("5");
        System.err.flush();

        final String log = new String(this.baos.toByteArray());

        Assert.assertTrue(log.contains("WARN"));
        Assert.assertTrue(log.contains("instance of"));
        Assert.assertTrue(log.contains("java.lang.String"));
        Assert.assertTrue(log.contains("which is declared"));

        Assert.assertTrue(log
                .contains("LoggerImplFactory set to com.google.code.morphia.logging.slf4j.SLF4JLogrImplFactory"));
    }

}

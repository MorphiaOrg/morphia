/*
 * Copyright (c) 2008-2015 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

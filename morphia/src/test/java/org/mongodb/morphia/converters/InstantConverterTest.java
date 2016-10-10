/*
 * Copyright 2016 MongoDB, Inc.
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

package org.mongodb.morphia.converters;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.Date;

@SuppressWarnings("Since15")
public class InstantConverterTest extends ConverterTest<Instant, Date> {
    public InstantConverterTest() {
        super(new InstantConverter());
    }

    @Before
    public void jdkVersionCheck() {
        Assume.assumeTrue(DefaultConverters.JAVA_8);
    }

    @Test
    public void convertNull() {
        Assert.assertNull(getConverter().decode(null, null));
        Assert.assertNull(getConverter().encode(null));
    }

    @Test
    public void testConversion() {
        Instant instant = Instant.ofEpochSecond(42);

        assertFormat(instant, new Date(42000));
        compare(Instant.class, instant);
    }
}

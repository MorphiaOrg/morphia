/*
 * Copyright (c) 2008-2016 MongoDB, Inc.
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

package dev.morphia.converters;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.text.ParseException;
import java.time.LocalTime;
import java.util.Random;

@SuppressWarnings("Since15")
public class LocalTimeConverterTest extends ConverterTest<LocalTime, Long> {
    public LocalTimeConverterTest() {
        super(new LocalTimeConverter());
    }

    @Test
    public void convertNull() {
        Assert.assertNull(getConverter().decode(null, null));
        Assert.assertNull(getConverter().encode(null));
    }

    @Test
    public void spanClock() {
        Random random = new Random();
        for (int hour = 0; hour < 23; hour++) {
            for (int minute = 0; minute < 60; minute++) {
                for (int second = 0; second < 60; second++) {
                    compare(LocalTime.class, LocalTime.of(hour, minute, second, random.nextInt(1000) * 1000000));
                }
            }
        }
    }

    @Test
    @Ignore("windows specific test failure")
    public void testConversion() throws ParseException {
        final LocalTime time = LocalTime.of(12, 30, 45);

        compare(LocalTime.class, LocalTime.now());
        assertFormat(time, 45045000L);
    }

}

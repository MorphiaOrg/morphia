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

package dev.morphia.converters;

import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;

@SuppressWarnings("Since15")
public class LocalDateConverterTest extends ConverterTest<LocalDate, Date> {
    public LocalDateConverterTest() {
        super(null);
        setConverter(new LocalDateConverter(getDs().getMapper()));
    }

    @Test
    public void convertNull() {
        Assert.assertNull(getConverter().decode(null, null));
        Assert.assertNull(getConverter().encode(null));
    }

    @Test
    public void testConversion() throws ParseException {
        Calendar cal = Calendar.getInstance();
        cal.set(2016, Calendar.MAY, 1, 0, 0, 0);
        cal.clear(Calendar.MILLISECOND);
        assertFormat(LocalDate.of(2016, 5, 1), cal.getTime());

        compare(LocalDate.class, LocalDate.now());

        compare(LocalDate.class, LocalDate.of(14478, 10, 15));
    }
}

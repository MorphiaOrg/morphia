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

import org.bson.types.Decimal128;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class BigDecimalConverterTest extends ConverterTest<BigDecimal, Decimal128> {
    private BigDecimalConverter converter = new BigDecimalConverter();

    public BigDecimalConverterTest() {
        super(new BigDecimalConverter());
    }

    @Test
    public void convertNull() {
        Assert.assertNull(getConverter().decode(null, null));
        Assert.assertNull(getConverter().encode(null));
    }

    @Test
    public void decodes() {
        compare(BigDecimal.class, new BigDecimal("12345678901234567890"));

        assertEquals(new BigDecimal(42L), converter.decode(BigDecimal.class, new BigDecimal(42L)));
        assertEquals(new BigDecimal("12345678901234567890"), converter.decode(BigDecimal.class, new BigInteger("12345678901234567890")));
        assertEquals(new BigDecimal(42L), converter.decode(BigDecimal.class, 42L));
        assertEquals(new BigDecimal(Long.valueOf(42)), converter.decode(BigDecimal.class, 42L));
        assertEquals(new BigDecimal(42D), converter.decode(BigDecimal.class, 42D));
        assertEquals(new BigDecimal(Double.valueOf(42)), converter.decode(BigDecimal.class, 42D));
        assertEquals(new BigDecimal("12345678901234567890"), converter.decode(BigDecimal.class, "12345678901234567890"));
        assertEquals(new BigDecimal("1.2345678901234567890"), converter.decode(BigDecimal.class, "1.2345678901234567890"));

        assertEquals(new BigDecimal(Double.MAX_VALUE), converter.decode(BigDecimal.class, Double.MAX_VALUE));
        assertEquals(new BigDecimal(Double.MIN_VALUE), converter.decode(BigDecimal.class, Double.MIN_VALUE));

        assertEquals(new BigDecimal(Long.MAX_VALUE), converter.decode(BigDecimal.class, Long.MAX_VALUE));
        assertEquals(new BigDecimal(Long.MIN_VALUE), converter.decode(BigDecimal.class, Long.MIN_VALUE));

        assertEquals(new BigDecimal(0), converter.decode(BigDecimal.class, 0));
        assertEquals(new BigDecimal(-0), converter.decode(BigDecimal.class, -0));
    }

    @Test
    public void testConversion() {
        compare(BigDecimal.class, new BigDecimal("12345678901234567890"));

        compare(BigDecimal.class, new BigDecimal(42L));
        compare(BigDecimal.class, new BigDecimal(Long.valueOf(42)));
        compare(BigDecimal.class, new BigDecimal(42D));
        compare(BigDecimal.class, new BigDecimal(Double.valueOf(42)));
        compare(BigDecimal.class, new BigDecimal("12345678901234567890"));
        compare(BigDecimal.class, new BigDecimal("1.2345678901234567890"));
        compare(BigDecimal.class, new BigDecimal("0"));
        compare(BigDecimal.class, new BigDecimal("-0"));

        compare(BigDecimal.class, new BigDecimal(Long.MAX_VALUE));
        compare(BigDecimal.class, new BigDecimal(Long.MIN_VALUE));

        compare(BigDecimal.class, new BigDecimal(0));
        compare(BigDecimal.class, new BigDecimal(-0));
    }

}

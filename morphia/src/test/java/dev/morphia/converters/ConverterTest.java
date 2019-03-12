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
import dev.morphia.TestBase;

import static java.lang.String.format;

class ConverterTest<F, T> extends TestBase {
    private TypeConverter converter;

    ConverterTest(final TypeConverter converter) {
        this.converter = converter;
    }

    void assertFormat(final F input, final T expected) {
        Assert.assertEquals(expected, getConverter().encode(input));
    }

    protected TypeConverter getConverter() {
        return converter;
    }

    protected void setConverter(final TypeConverter converter) {
        this.converter = converter;
    }

    @SuppressWarnings("unchecked")
    void compare(final Class<F> targetClass, final F value) {
        compare(targetClass, (T) converter.encode(value), value);
    }

    void compare(final Class<F> targetClass, final T encoded, final F value) {
        final Object decoded = converter.decode(targetClass, encoded);
        Assert.assertEquals(format("%s didn't survive the round trip: decoded = %s encoded=%s", value, decoded, encoded),
                            value, decoded);
    }
}

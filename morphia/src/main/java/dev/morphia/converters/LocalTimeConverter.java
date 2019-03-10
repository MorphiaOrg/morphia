/*
 * Copyright (c) 2016 MongoDB, Inc.
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

import dev.morphia.mapping.MappedField;

import java.time.LocalTime;

/**
 * Provides a converter for {@link LocalTime} and convert it to its numeric form of milliseconds since midnight.
 */
@SuppressWarnings("Since15")
public class LocalTimeConverter extends TypeConverter implements SimpleValueConverter {

    private static final int MILLI_MODULO = 1000000;

    /**
     * Creates the Converter.
     */
    public LocalTimeConverter() {
        super(LocalTime.class);
    }

    @Override
    public Object decode(final Class<?> targetClass, final Object val, final MappedField optionalExtraInfo) {
        if (val == null) {
            return null;
        }

        if (val instanceof LocalTime) {
            return val;
        }

        if (val instanceof Number) {
            return LocalTime.ofNanoOfDay(((Number) val).longValue() * MILLI_MODULO);
        }

        throw new IllegalArgumentException("Can't convert to LocalTime from " + val);
    }

    @Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        if (value == null) {
            return null;
        }
        LocalTime time = (LocalTime) value;

        return time.toNanoOfDay() / MILLI_MODULO;
    }
}

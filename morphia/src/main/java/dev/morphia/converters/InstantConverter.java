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

import java.time.Instant;
import java.util.Date;

/**
 * This converter will take a {@link Instant} and convert it to a java.util.Date instance.
 */
@SuppressWarnings("Since15")
public class InstantConverter extends TypeConverter implements SimpleValueConverter {

    /**
     * Creates the Converter.
     */
    public InstantConverter() {
        super(Instant.class);
    }

    @Override
    public Object decode(final Class<?> targetClass, final Object val, final MappedField optionalExtraInfo) {
        if (val == null) {
            return null;
        }

        if (val instanceof Instant) {
            return val;
        }

        if (val instanceof Date) {
            return ((Date) val).toInstant();
        }

        throw new IllegalArgumentException("Can't convert to Instant from " + val);
    }

    @Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        if (value == null) {
            return null;
        }
        return Date.from((Instant) value);
    }
}

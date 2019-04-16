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
import dev.morphia.mapping.Mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Provides a converter for {@link LocalDate} converting the value to the Date at the start of that day.
 */
@SuppressWarnings("Since15")
public class LocalDateConverter extends TypeConverter implements SimpleValueConverter {
    private Mapper mapper;

    /**
     * Creates the Converter.
     *
     * @param mapper the mapper
     */
    public LocalDateConverter(final Mapper mapper) {
        super(LocalDate.class);
        this.mapper = mapper;
    }

    @Override
    public Object decode(final Class<?> targetClass, final Object val, final MappedField optionalExtraInfo) {
        if (val == null) {
            return null;
        }

        if (val instanceof LocalDate) {
            return val;
        }

        if (val instanceof Date) {
            return LocalDateTime.ofInstant(((Date) val).toInstant(), mapper.getOptions().getDateStorage().getZone())
                                .toLocalDate();
        }

        throw new IllegalArgumentException("Can't convert to LocalDate from " + val);
    }

    @Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        if (value == null) {
            return null;
        }
        LocalDate date = (LocalDate) value;
        return Date.from(date.atStartOfDay()
                             .atZone(mapper.getOptions().getDateStorage().getZone())
                             .toInstant());
    }
}

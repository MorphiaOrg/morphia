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

import org.bson.types.Decimal128;
import dev.morphia.mapping.MappedField;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * This provides a conversion to/from BigDecimal in applications using the Decimal128 type introduced in the 3.4.0 release of MongoDB.  This
 * converter also supports loading certain types of values from the database and converting them to BigDecimal.
 * <p>
 * Note:  While this is useful for gradual model migrations, the values will be saved as Decimal128 types when saved back.  This will
 * result in disparate types for a given field which may yield unexpected results when querying.
 *
 * @mongodb.server.release 3.4
 * @see Decimal128
 * @since 1.3
 */
public class BigDecimalConverter extends TypeConverter implements SimpleValueConverter {

    /**
     * Creates the Converter.
     */
    public BigDecimalConverter() {
        super(BigDecimal.class);
    }

    @Override
    public Object decode(final Class<?> targetClass, final Object value, final MappedField optionalExtraInfo) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal) {
            return value;
        }

        if (value instanceof Decimal128) {
            return ((Decimal128) value).bigDecimalValue();
        }

        if (value instanceof BigInteger) {
            return new BigDecimal(((BigInteger) value));
        }

        if (value instanceof Double) {
            return new BigDecimal(((Double) value));
        }

        if (value instanceof Long) {
            return new BigDecimal(((Long) value));
        }

        if (value instanceof Number) {
            return new BigDecimal(((Number) value).doubleValue());
        }

        return new BigDecimal(value.toString());
    }

    @Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        if (value instanceof BigDecimal) {
            return new Decimal128((BigDecimal) value);
        }
        return super.encode(value, optionalExtraInfo);
    }
}

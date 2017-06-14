/*
 * Copyright 2017 MongoDB, Inc.
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

import org.mongodb.morphia.mapping.MappedField;

import java.util.Currency;
import java.util.Locale;

/**
 * @author Adam Dispenza, (adam@dispenza.org)
 */
public class CurrencyConverter extends TypeConverter implements SimpleValueConverter {
    /**
     * Creates the Converter.
     */
    public CurrencyConverter() {
        super(Currency.class);
    }

    @Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        return isValidCurrency(value) ? ((Currency) value).getCurrencyCode() : null;
    }

    @Override
    public Object decode(final Class<?> targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) {
        if (fromDBObject == null) {
            return null;
        }

        if (fromDBObject instanceof Locale) {
            return Currency.getInstance((Locale) fromDBObject);
        }

        return Currency.getInstance((String) fromDBObject);
    }

    private boolean isValidCurrency(final Object potentialCurrency) {
        return potentialCurrency != null && potentialCurrency instanceof Currency;
    }
}


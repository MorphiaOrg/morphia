package org.mongodb.morphia.converters;


import org.mongodb.morphia.mapping.MappedField;

import java.util.Date;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
public class DateConverter extends TypeConverter implements SimpleValueConverter {

    public DateConverter() {
        this(Date.class);
    }

    protected DateConverter(final Class clazz) {
        super(clazz);
    }

    @Override
    public Object decode(final Class targetClass, final Object val, final MappedField optionalExtraInfo) {
        if (val == null) {
            return null;
        }

        if (val instanceof Date) {
            return val;
        }

        if (val instanceof Number) {
            return new Date(((Number) val).longValue());
        }

        return new Date(Date.parse(val.toString())); // good luck
    }
}

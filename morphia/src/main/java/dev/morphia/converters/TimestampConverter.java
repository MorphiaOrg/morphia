package dev.morphia.converters;


import dev.morphia.mapping.MappedField;

import java.sql.Timestamp;
import java.util.Date;


/**
 * @author scotthernandez
 */
public class TimestampConverter extends DateConverter {

    /**
     * Creates the Converter.
     */
    public TimestampConverter() {
        super(Timestamp.class);
    }

    @Override
    public Object decode(final Class targetClass, final Object val, final MappedField optionalExtraInfo) {
        final Date d = (Date) super.decode(targetClass, val, optionalExtraInfo);
        return new Timestamp(d.getTime());
    }

    @Override
    public Object encode(final Object val, final MappedField optionalExtraInfo) {
        if (val == null) {
            return null;
        }
        return new Date(((Timestamp) val).getTime());
    }
}

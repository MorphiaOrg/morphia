package org.mongodb.morphia.converters;


import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.mapping.MappedField;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
public class DateConverter extends TypeConverter implements SimpleValueConverter {
    private static final Logger LOG = MorphiaLoggerFactory.get(DateConverter.class);


    public DateConverter() {
        this(Date.class);
    }

    protected DateConverter(final Class clazz) {
        super(clazz);
    }

    @Override
    public Object decode(final Class<?> targetClass, final Object val, final MappedField optionalExtraInfo) {
        if (val == null) {
            return null;
        }

        if (val instanceof Date) {
            return val;
        }

        if (val instanceof Number) {
            return new Date(((Number) val).longValue());
        }

        if (val instanceof String) {
            try {
                return new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy").parse((String) val);
            } catch (ParseException e) {
                LOG.error("Can't parse Date from: " + val);
            }

        }
        
        throw new IllegalArgumentException("Can't convert to Date from " + val);
    }
}

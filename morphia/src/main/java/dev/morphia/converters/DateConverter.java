package dev.morphia.converters;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.morphia.mapping.MappedField;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
public class DateConverter extends TypeConverter implements SimpleValueConverter {
    private static final Logger LOG = LoggerFactory.getLogger(DateConverter.class);


    /**
     * Creates the Converter.
     */
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
                return new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy", Locale.US).parse((String) val);
            } catch (ParseException e) {
                LOG.error("Can't parse Date from: " + val);
            }

        }

        throw new IllegalArgumentException("Can't convert to Date from " + val);
    }
}

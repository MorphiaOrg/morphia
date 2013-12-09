package org.mongodb.morphia.converters;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.mongodb.morphia.logging.Logr;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.mapping.MappedField;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
public class DateConverter extends TypeConverter implements SimpleValueConverter {
	private static final Logr LOG = MorphiaLoggerFactory.get(DateConverter.class);

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

		if (val instanceof String) {
			DateFormat df = new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy");
			try {
				return df.parse((String) val);
			} catch (ParseException e) {
				LOG.error("Can't unparse Date from String: " + val);
			}
		}

		return new Date(Date.parse(val.toString())); // good luck
	}
}

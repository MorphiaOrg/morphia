package org.mongodb.morphia.converters;


import java.lang.reflect.Array;

import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.MappingException;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
@SuppressWarnings({"rawtypes"})
public class CharacterConverter extends TypeConverter implements SimpleValueConverter {
    public CharacterConverter() {
        super(char.class, Character.class);
    }

    @Override
    public Object decode(final Class targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) throws MappingException {
        if (fromDBObject == null) {
            return null;
        }

        if (fromDBObject instanceof String) {
            final char[] chars = ((String) fromDBObject).toCharArray();
            if (chars.length == 1) {
                return chars[0];
            } else if (chars.length == 0) {
                return (char)0;
            }
        }
        throw new MappingException("Trying to map multi-character data to a single character: " + fromDBObject);
    }

    @Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        return value == null || value.equals('\0') ? null : String.valueOf(value);
    }

    private Object convert(final Class<?> type, final String[] values) {
        final Object array = Array.newInstance(type, values.length);
        for (int i = 0; i < values.length; i++) {
            Array.set(array, i, decode(type, values[i]));
        }
        return array;
    }

    public static Object convert(final Class type, final char[] values) {
        final Object array = Array.newInstance(type, values.length);
        for (int i = 0; i < values.length; i++) {
            Array.set(array, i, values[i]);
        }
        return array;
    }
}

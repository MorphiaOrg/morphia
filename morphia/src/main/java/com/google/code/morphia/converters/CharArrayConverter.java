package com.google.code.morphia.converters;


import java.lang.reflect.Array;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 * @deprecated replaced by CharacterConverter
 */
@SuppressWarnings({"rawtypes"})
public class CharArrayConverter extends TypeConverter implements SimpleValueConverter {
    public CharArrayConverter() {
        super(char[].class, Character[].class);
    }

    @Override
    public Object decode(final Class targetClass, final Object val, final MappedField optionalExtraInfo) throws MappingException {
        if (val == null) {
            return null;
        }

        final char[] chars = val.toString().toCharArray();
        if (targetClass.isArray() && targetClass.equals(Character[].class)) {
            return convertToWrapperArray(chars);
        }
        return chars;
    }

    public static Object convertToWrapperArray(final char[] values) {
        final int length = values.length;
        final Object array = Array.newInstance(Character.class, length);
        for (int i = 0; i < length; i++) {
            Array.set(array, i, new Character(values[i]));
        }
        return array;
    }

    @Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        if (value == null) {
            return null;
        } else {
            if (value instanceof char[]) {
                return new String((char[]) value);
            } else {
                final StringBuilder builder = new StringBuilder();
                final Character[] array = (Character[]) value;
                for (final Character character : array) {
                    builder.append(character);
                }
                return builder.toString();
            }
        }
    }
}

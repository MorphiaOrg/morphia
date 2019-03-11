package dev.morphia.converters;


import dev.morphia.mapping.MappedField;

import java.lang.reflect.Array;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
public class ByteConverter extends TypeConverter implements SimpleValueConverter {
    /**
     * Creates the Converter.
     */
    public ByteConverter() {
        super(byte.class, Byte.class, byte[].class, Byte[].class);
    }

    @Override
    public Object decode(final Class targetClass, final Object val, final MappedField optionalExtraInfo) {
        if (val == null) {
            return null;
        }

        if (val.getClass().equals(targetClass)) {
            return val;
        }

        if (val instanceof Number) {
            return ((Number) val).byteValue();
        }

        if (targetClass.isArray() && val.getClass().equals(byte[].class)) {
            return convertToWrapperArray((byte[]) val);
        }
        return Byte.parseByte(val.toString());
    }

    @Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        if (value instanceof Byte[]) {
            return super.encode(convertToPrimitiveArray((Byte[]) value), optionalExtraInfo);
        }
        return super.encode(value, optionalExtraInfo);
    }

    Object convertToPrimitiveArray(final Byte[] values) {
        final int length = values.length;
        final Object array = Array.newInstance(byte.class, length);
        for (int i = 0; i < length; i++) {
            Array.set(array, i, values[i]);
        }
        return array;
    }

    Object convertToWrapperArray(final byte[] values) {
        final int length = values.length;
        final Object array = Array.newInstance(Byte.class, length);
        for (int i = 0; i < length; i++) {
            Array.set(array, i, values[i]);
        }
        return array;
    }
}

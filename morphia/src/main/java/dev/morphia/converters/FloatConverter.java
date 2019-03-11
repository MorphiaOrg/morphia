package dev.morphia.converters;

import dev.morphia.mapping.MappedField;

import java.lang.reflect.Array;
import java.util.List;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
public class FloatConverter extends TypeConverter implements SimpleValueConverter {

    /**
     * Creates the Converter.
     */
    public FloatConverter() {
        super(float.class, Float.class, float[].class, Float[].class);
    }

    @Override
    public Object decode(final Class targetClass, final Object val, final MappedField optionalExtraInfo) {
        if (val == null) {
            return null;
        }

        if (val instanceof Float) {
            return val;
        }

        if (val instanceof Number) {
            return ((Number) val).floatValue();
        }

        if (val instanceof List) {
            final Class<?> type = targetClass.isArray() ? targetClass.getComponentType() : targetClass;
            return convertToArray(type, (List<?>) val);
        }

        return Float.parseFloat(val.toString());
    }

    private Object convertToArray(final Class type, final List<?> values) {
        final Object array = Array.newInstance(type, values.size());
        try {
            return values.toArray((Object[]) array);
        } catch (Exception e) {
            for (int i = 0; i < values.size(); i++) {
                Array.set(array, i, decode(Float.class, values.get(i)));
            }
            return array;
        }
    }
}

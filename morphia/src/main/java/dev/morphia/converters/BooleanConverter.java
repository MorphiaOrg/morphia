package dev.morphia.converters;


import dev.morphia.mapping.MappedField;
import dev.morphia.utils.ReflectionUtils;

import java.util.List;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
public class BooleanConverter extends TypeConverter implements SimpleValueConverter {

    /**
     * Creates the Converter.
     */
    public BooleanConverter() {
        super(boolean.class, Boolean.class, boolean[].class, Boolean[].class);
    }

    @Override
    public Object decode(final Class targetClass, final Object val, final MappedField optionalExtraInfo) {
        if (val == null) {
            return null;
        }

        if (val instanceof Boolean) {
            return val;
        }

        //handle the case for things like the ok field
        if (val instanceof Number) {
            return ((Number) val).intValue() != 0;
        }

        if (val instanceof List) {
            final Class<?> type = targetClass.isArray() ? targetClass.getComponentType() : targetClass;
            return ReflectionUtils.convertToArray(type, (List<?>) val);
        }

        return Boolean.parseBoolean(val.toString());
    }
}

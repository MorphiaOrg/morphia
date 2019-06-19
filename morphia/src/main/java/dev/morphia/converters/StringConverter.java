package dev.morphia.converters;


import dev.morphia.mapping.MappedField;
import dev.morphia.utils.ReflectionUtils;

import java.util.List;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
public class StringConverter extends TypeConverter implements SimpleValueConverter {
    /**
     * Creates the Converter.
     */
    public StringConverter() {
        super(String.class, String[].class);
    }

    @Override
    public Object decode(final Class targetClass, final Object fromDocument, final MappedField optionalExtraInfo) {
        if (fromDocument == null) {
            return null;
        }

        if (targetClass.equals(fromDocument.getClass())) {
            return fromDocument;
        }

        if (fromDocument instanceof List) {
            final Class<?> type = targetClass.isArray() ? targetClass.getComponentType() : targetClass;
            return ReflectionUtils.convertToArray(type, (List<?>) fromDocument);
        }

        if (targetClass.equals(String[].class)) {
            return new String[]{fromDocument.toString()};
        }

        return fromDocument.toString();
    }
}

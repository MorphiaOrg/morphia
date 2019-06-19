package dev.morphia.converters;


import dev.morphia.mapping.MappedField;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
public class EnumConverter extends TypeConverter implements SimpleValueConverter {

    @Override
    @SuppressWarnings("unchecked")
    public Object decode(final Class targetClass, final Object fromDocument, final MappedField optionalExtraInfo) {
        if (fromDocument == null) {
            return null;
        }
        return Enum.valueOf(targetClass, fromDocument.toString());
    }

    @Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        if (value == null) {
            return null;
        }

        return getName((Enum) value);
    }

    @Override
    protected boolean isSupported(final Class c, final MappedField optionalExtraInfo) {
        return c.isEnum();
    }

    private <T extends Enum> String getName(final T value) {
        return value.name();
    }
}

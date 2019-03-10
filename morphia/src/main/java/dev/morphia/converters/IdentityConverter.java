package dev.morphia.converters;


import dev.morphia.mapping.MappedField;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
public class IdentityConverter extends TypeConverter {

    /**
     * Creates the Converter.
     *
     * @param types the types to pass through this converter
     */
    public IdentityConverter(final Class... types) {
        super(types);
    }

    @Override
    public Object decode(final Class targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) {
        return fromDBObject;
    }

    @Override
    protected boolean isSupported(final Class c, final MappedField optionalExtraInfo) {
        return true;
    }
}

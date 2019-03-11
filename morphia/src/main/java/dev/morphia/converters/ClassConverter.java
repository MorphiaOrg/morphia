package dev.morphia.converters;


import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.MappingException;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
public class ClassConverter extends TypeConverter implements SimpleValueConverter {

    /**
     * Creates the Converter.
     */
    public ClassConverter() {
        super(Class.class);
    }

    @Override
    public Object decode(final Class targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) {
        if (fromDBObject == null) {
            return null;
        }

        final String l = fromDBObject.toString();
        try {
            return Class.forName(l);
        } catch (ClassNotFoundException e) {
            throw new MappingException("Cannot create class from Name '" + l + "'", e);
        }
    }

    @Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        if (value == null) {
            return null;
        } else {
            return ((Class) value).getName();
        }
    }
}

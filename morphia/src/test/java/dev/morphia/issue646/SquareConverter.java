package dev.morphia.issue646;

import dev.morphia.converters.SimpleValueConverter;
import dev.morphia.converters.TypeConverter;
import dev.morphia.mapping.MappedField;

public class SquareConverter extends TypeConverter implements SimpleValueConverter {

    public SquareConverter() {
        super(Square.class);
    }

    @Override
    public Object decode(final Class targetClass, final Object fromDocument, final MappedField optionalExtraInfo) {
        if (fromDocument == null) {
            return null;
        }

        if (fromDocument instanceof String) {
            return new Square();
        }

        throw new RuntimeException(
                                      "Did not expect " + fromDocument.getClass().getName());
    }

    @Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        if (value == null) {
            return null;
        }

        if (!(value instanceof Square)) {
            throw new RuntimeException(
                                          "Did not expect " + value.getClass().getName());
        }

        return "Square";
    }
}

package dev.morphia.geo;

import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;

/**
 * Converts Point objects into Documents for querying only.  When saving entities with Points in, this converter should not be used.
 */
public class GeometryQueryConverter {

    /**
     * Create a new converter.  Registers itself to convert Point classes.
     *
     * @param mapper the Mapper is required as this converter delegates other type encoding back to the mapper
     */
    public GeometryQueryConverter(final Mapper mapper) {
    }

    public Object decode(final Class<?> targetClass, final Object fromDocument, final MappedField optionalExtraInfo) {
        throw new UnsupportedOperationException("Should never have to decode a query object");
    }

    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        if (1 == 1) {
            //TODO:  implement this
            throw new UnsupportedOperationException();
        }
        return null;
        //        Object encode = getMapper().getConverters().encode(value);
        //        return new Document("$geometry", encode);
    }
}

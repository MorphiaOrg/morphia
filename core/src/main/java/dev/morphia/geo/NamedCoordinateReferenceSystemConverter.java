package dev.morphia.geo;

/**
 * A Morphia TypeConverter that knows how to turn things that are labelled with the Geometry interface into the correct concrete class,
 * based on the GeoJSON type.
 * <p/>
 * Only implements the decode method as the concrete classes can encode themselves without needing a converter. It's when they come out of
 * the database that there's not enough information for Morphia to automatically create Geometry instances.
 * @deprecated use the driver-provided types instead
 */
@Deprecated(since = "2.0", forRemoval = true)
public class NamedCoordinateReferenceSystemConverter {
/*
    public Object decode(final Class<?> targetClass, final Object fromDocument, final MappedField optionalExtraInfo) {
        throw new UnsupportedOperationException("We should never need to decode these");
    }

    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        NamedCoordinateReferenceSystem crs = (NamedCoordinateReferenceSystem) value;

        return new Document("type", crs.getType().getTypeName())
                   .append("properties", new Document("name", crs.getName()));
    }

    protected boolean isSupported(final Class<?> c, final MappedField optionalExtraInfo) {
        return CoordinateReferenceSystem.class.isAssignableFrom(c);
    }
*/
}

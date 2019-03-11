package dev.morphia.geo;

import com.mongodb.BasicDBObject;
import dev.morphia.converters.SimpleValueConverter;
import dev.morphia.converters.TypeConverter;
import dev.morphia.mapping.MappedField;

/**
 * A Morphia TypeConverter that knows how to turn things that are labelled with the Geometry interface into the correct concrete class,
 * based on the GeoJSON type.
 * <p/>
 * Only implements the decode method as the concrete classes can encode themselves without needing a converter. It's when they come out of
 * the database that there's not enough information for Morphia to automatically create Geometry instances.
 */
public class NamedCoordinateReferenceSystemConverter extends TypeConverter implements SimpleValueConverter {
    /**
     * Sets up this converter to work with things that implement the Geometry interface
     */
    public NamedCoordinateReferenceSystemConverter() {
        super(NamedCoordinateReferenceSystem.class);
    }

    @Override
    public Object decode(final Class<?> targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) {
        throw new UnsupportedOperationException("We should never need to decode these");
    }

    @Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        NamedCoordinateReferenceSystem crs = (NamedCoordinateReferenceSystem) value;
        final BasicDBObject dbObject = new BasicDBObject("type", crs.getType().getTypeName());
        dbObject.put("properties", new BasicDBObject("name", crs.getName()));

        return dbObject;
    }

    @Override
    protected boolean isSupported(final Class<?> c, final MappedField optionalExtraInfo) {
        return CoordinateReferenceSystem.class.isAssignableFrom(c);
    }
}

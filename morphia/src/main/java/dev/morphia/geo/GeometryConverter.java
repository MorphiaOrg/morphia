package dev.morphia.geo;

import dev.morphia.mapping.MappedField;
import org.bson.Document;

/**
 * A Morphia TypeConverter that knows how to turn things that are labelled with the Geometry interface into the correct concrete class,
 * based on the GeoJSON type.
 * <p/>
 * Only implements the decode method as the concrete classes can encode themselves without needing a converter. It's when they come out of
 * the database that there's not enough information for Morphia to automatically create Geometry instances.
 */
public class GeometryConverter  {

    public Object decode(final Class<?> targetClass, final Object fromDocument, final MappedField optionalExtraInfo) {
        if (1 == 1) {
            //TODO:  implement this
            throw new UnsupportedOperationException();
        }

        Document dbObject = (Document) fromDocument;
        String type = (String) dbObject.get("type");
//        return getMapper().getConverters().decode(GeoJsonType.fromString(type).getTypeClass(), fromDocument, optionalExtraInfo);

        return null;
    }
}

package org.mongodb.morphia.geo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mongodb.morphia.geo.GeoJsonType.LINE_STRING;
import static org.mongodb.morphia.geo.GeoJsonType.MULTI_LINE_STRING;
import static org.mongodb.morphia.geo.GeoJsonType.MULTI_POINT;
import static org.mongodb.morphia.geo.GeoJsonType.MULTI_POLYGON;
import static org.mongodb.morphia.geo.GeoJsonType.POINT;
import static org.mongodb.morphia.geo.GeoJsonType.POLYGON;

/**
 * Converter that understands most Geometry instances are effectively just lists of either other geometry objects or double coordinates.
 * Recursively encodes and decodes Geometry objects, but needs to be instantiated with a List of GeometryFactory instances that represented
 * the hierarchy of Geometries that make up the required Geometry object.
 * <p/>
 * Overridden by subclasses to define exact behaviour for specific Geometry concrete classes.
 */
public class GeometryShapeConverter extends TypeConverter implements SimpleValueConverter {
    private final GeoJsonType geoJsonType;
    private final List<GeometryFactory> factories;

    GeometryShapeConverter(final GeoJsonType... geoJsonTypes) {
        super(geoJsonTypes[0].getTypeClass());
        geoJsonType = geoJsonTypes[0];
        this.factories = Arrays.<GeometryFactory>asList(geoJsonTypes);
    }

    @Override
    public Object decode(final Class<?> targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) {
        return decodeObject(((DBObject) fromDBObject).get("coordinates"), factories);
    }

    @Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        if (value != null) {
            Object encodedObjects = encodeObjects(((Geometry) value).getCoordinates());
            return new BasicDBObject("type", geoJsonType.getType())
                       .append("coordinates", encodedObjects);
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked") // always have unchecked casts when dealing with raw classes
    private Object decodeObject(final Object fromDBObject, final List<GeometryFactory> geometryFactories) {
        if (!geometryFactories.isEmpty()) {
            // we're expecting a list that can be turned into a geometry using one of these factories
            GeometryFactory factory = geometryFactories.get(0);
            if (fromDBObject instanceof List) {
                List<Object> decodedObjects = new ArrayList<Object>();
                for (final Object objectThatNeedsDecoding : (List) fromDBObject) {
                    decodedObjects.add(decodeObject(objectThatNeedsDecoding, geometryFactories.subList(1, geometryFactories.size())));
                }
                return factory.createGeometry(decodedObjects);
            }
        }
        return getMapper().getConverters().encode(fromDBObject);
    }

    private Object encodeObjects(final List value) {
        List<Object> encodedObjects = new ArrayList<Object>();
        for (final Object object : value) {
            if (object instanceof Geometry) {
                //iterate through the list of geometry objects recursively until you find the lowest-level
                encodedObjects.add(encodeObjects(((Geometry) object).getCoordinates()));
            } else {
                encodedObjects.add(getMapper().getConverters().encode(object));
            }
        }
        return encodedObjects;
    }

    /**
     * Extends and therefore configures GeometryShapeConverter to provide the specific configuration for converting MultiPolygon objects to
     * and from <a href="http://geojson.org/geojson-spec.html#id7">MongoDB representations</a> of the GeoJson.
     */
    public static class MultiPolygonConverter extends GeometryShapeConverter {
        /**
         * Creates a new MultiPolygonConverter.
         */
        public MultiPolygonConverter() {
            super(MULTI_POLYGON, POLYGON, LINE_STRING, POINT);
        }
    }

    /**
     * Defines a new PolygonConverter.  This extends and therefore configures GeometryShapeConverter to provide the specific
     * configuration for converting Polygon objects to and from <a href="http://geojson.org/geojson-spec.html#id4">MongoDB
     * representations</a> of the GeoJson.
     */
    public static class PolygonConverter extends GeometryShapeConverter {
        /**
         * Creates a new PolygonConverter.
         */
        public PolygonConverter() {
            super(POLYGON, LINE_STRING, POINT);
        }
    }

    /**
     * Defines a new MultiLineStringConverter.  This extends and therefore configures GeometryShapeConverter to provide the specific
     * configuration for converting MultiLineString objects to and from <a href="http://geojson.org/geojson-spec.html#id6">MongoDB
     * representations</a> of the GeoJson.
     */
    public static class MultiLineStringConverter extends GeometryShapeConverter {
        /**
         * Creates a new MultiLineStringConverter.
         */
        public MultiLineStringConverter() {
            super(MULTI_LINE_STRING, LINE_STRING, POINT);
        }
    }

    /**
     * Defines a new MultiPointConverter. This extends and therefore configures GeometryShapeConverter to provide the specific
     * configuration for converting MultiPoint objects to and from <a href="http://geojson.org/geojson-spec.html#id5">MongoDB
     * representations</a> of the GeoJson.
     */
    public static class MultiPointConverter extends GeometryShapeConverter {
        /**
         * Creates a new MultiPointConverter.
         */
        public MultiPointConverter() {
            super(MULTI_POINT, POINT);
        }
    }

    /**
     * Defines a new LineStringConverter. This extends and therefore configures GeometryShapeConverter to provide the specific
     * configuration for converting LineString objects to and from <a href="http://geojson.org/geojson-spec.html#id3">MongoDB
     * representations</a> of the GeoJson.
     */
    public static class LineStringConverter extends GeometryShapeConverter {
        /**
         * Creates a new LineStringConverter.
         */
        public LineStringConverter() {
            super(LINE_STRING, POINT);
        }
    }

    /**
     * Defines a new PointConverter. This extends and therefore configures GeometryShapeConverter to provide the specific configuration
     * for converting Point objects to and from <a href="http://geojson.org/geojson-spec.html#id3">MongoDB representations</a> of the
     * GeoJson.
     */
    public static class PointConverter extends GeometryShapeConverter {
        /**
         * Creates a new PointConverter.
         */
        public PointConverter() {
            super(POINT);
        }
    }
}

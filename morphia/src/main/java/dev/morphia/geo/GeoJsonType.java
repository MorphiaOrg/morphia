package dev.morphia.geo;

import java.util.List;

/**
 * Enumerates all the GeoJson types that are currently supported by Morphia.
 * @deprecated use the driver defined types
 */
@SuppressWarnings({"unchecked", "removal"}) // can't know, or define generics for, the Lists in the factory
@Deprecated(since = "2.0", forRemoval = true)
public enum GeoJsonType implements GeometryFactory {
    POINT("Point", Point.class) {
        @Override
        public Geometry createGeometry(final List coordinates) {
            return new Point(coordinates);
        }
    },
    LINE_STRING("LineString", LineString.class) {
        @Override
        public Geometry createGeometry(final List objects) {
            return new LineString(objects);
        }
    },
    POLYGON("Polygon", Polygon.class) {
        @Override
        public Geometry createGeometry(final List boundaries) {
            return new Polygon(boundaries);
        }
    },
    MULTI_POINT("MultiPoint", MultiPoint.class) {
        @Override
        public Geometry createGeometry(final List points) {
            return new MultiPoint(points);
        }
    },
    MULTI_LINE_STRING("MultiLineString", MultiLineString.class) {
        @Override
        public Geometry createGeometry(final List lineStrings) {
            return new MultiLineString(lineStrings);
        }
    },
    MULTI_POLYGON("MultiPolygon", MultiPolygon.class) {
        @Override
        public Geometry createGeometry(final List polygons) {
            return new MultiPolygon(polygons);
        }
    };

    private final String type;
    private final Class<? extends Geometry> typeClass;

    GeoJsonType(final String type, final Class<? extends Geometry> typeClass) {
        this.type = type;
        this.typeClass = typeClass;
    }

}

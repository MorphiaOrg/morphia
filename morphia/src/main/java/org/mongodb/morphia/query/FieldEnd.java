package org.mongodb.morphia.query;


import org.mongodb.morphia.geo.CoordinateReferenceSystem;
import org.mongodb.morphia.geo.Geometry;
import org.mongodb.morphia.geo.MultiPolygon;
import org.mongodb.morphia.geo.Point;
import org.mongodb.morphia.geo.Polygon;

/**
 * Represents a document field in a query and presents the operations available to querying against that field.
 *
 * @param <T>
 */
public interface FieldEnd<T> {

    T contains(String string);

    T containsIgnoreCase(String suffix);

    T doesNotExist();

    T endsWith(String suffix);

    T endsWithIgnoreCase(String suffix);

    T equal(Object val);

    T exists();

    T greaterThan(Object val);

    T greaterThanOrEq(Object val);

    T hasAllOf(Iterable<?> values);

    T hasAnyOf(Iterable<?> values);

    T hasNoneOf(Iterable<?> values);

    T hasThisElement(Object val);

    T hasThisOne(Object val);

    T in(Iterable<?> values);

    /**
     * This performs a $geoIntersects query, searching documents containing any sort of GeoJson field and returning those where the given
     * geometry intersects with the document shape.  This includes cases where the data and the specified object share an edge.
     *
     * @param geometry the shape to use to query for any stored shapes that intersect
     * @return any documents where the GeoJson intersects with a specified {@code geometry}.
     */
    T intersects(Geometry geometry);

    /**
     * This performs a $geoIntersects query, searching documents containing any sort of GeoJson field and returning those where the given
     * geometry intersects with the document shape.  This includes cases where the data and the specified object share an edge.
     *
     * @param geometry the shape to use to query for any stored shapes that intersect
     * @param crs      the coordinate reference system to use with the query
     * @return any documents where the GeoJson intersects with a specified {@code geometry}.
     * @see <a href="http://docs.mongodb.org/manual/reference/operator/query/geometry/#op._S_geometry">$geometry</a>
     */
    T intersects(Geometry geometry, final CoordinateReferenceSystem crs);

    T lessThan(Object val);

    T lessThanOrEq(Object val);

    T mod(long divisor, long remainder);

    T near(double x, double y);

    T near(double x, double y, boolean spherical);

    T near(double x, double y, double radius);

    T near(double x, double y, double radius, boolean spherical);

    /**
     * This runs a $near query to check for documents geographically close to the given Point - this Point represents a GeoJSON point type.
     * These queries are only supported by MongoDB version 2.4 or greater.
     *
     * @param point       the point to find results close to
     * @param maxDistance the radius, in meters, to find the results inside
     * @return T
     */
    T near(Point point, int maxDistance);

    /**
     * This runs a $near query to check for documents geographically close to the given Point - this Point represents a GeoJSON point type.
     * These queries are only supported by MongoDB version 2.4 or greater.
     *
     * @param point the point to find results close to
     * @return T
     */
    T near(Point point);

    FieldEnd<T> not();

    T notEqual(Object val);

    T notIn(Iterable<?> values);

    T sizeEq(int val);

    T startsWith(String prefix);

    T startsWithIgnoreCase(final String prefix);

    T type(Type type);

    /**
     * This implements the $geoWithin operator and is only compatible with mongo 2.4 or greater.
     */
    T within(Shape shape);

    /**
     * This runs the $geoWithin query, returning documents with GeoJson fields whose area falls within the given boundary. When determining
     * inclusion, MongoDB considers the border of a shape to be part of the shape, subject to the precision of floating point numbers.
     * <p/>
     * These queries are only compatible with MongoDB 2.4 or greater.
     *
     * @param boundary a polygon describing the boundary to search within.
     * @return T
     */
    T within(Polygon boundary);

    /**
     * This runs the $geoWithin query, returning documents with GeoJson fields whose area falls within the given boundaries. When
     * determining inclusion, MongoDB considers the border of a shape to be part of the shape, subject to the precision of floating point
     * numbers.
     * <p/>
     * These queries are only compatible with MongoDB 2.6 or greater.
     *
     * @param boundaries a multi-polygon describing the areas to search within.
     * @return T
     */
    T within(MultiPolygon boundaries);

    /**
     * This runs the $geoWithin query, returning documents with GeoJson fields whose area falls within the given boundary. When determining
     * inclusion, MongoDB considers the border of a shape to be part of the shape, subject to the precision of floating point numbers.
     * <p/>
     * These queries are only compatible with MongoDB 2.4 or greater.
     *
     * @param boundary a polygon describing the boundary to search within.
     * @return T
     */
    T within(Polygon boundary, CoordinateReferenceSystem crs);

    /**
     * This runs the $geoWithin query, returning documents with GeoJson fields whose area falls within the given boundaries. When
     * determining inclusion, MongoDB considers the border of a shape to be part of the shape, subject to the precision of floating point
     * numbers.
     * <p/>
     * These queries are only compatible with MongoDB 2.6 or greater.
     *
     * @param boundaries a multi-polygon describing the areas to search within.
     * @return T
     */
    T within(MultiPolygon boundaries, CoordinateReferenceSystem crs);

}

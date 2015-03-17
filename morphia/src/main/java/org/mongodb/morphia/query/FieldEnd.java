package org.mongodb.morphia.query;


import org.mongodb.morphia.geo.MultiPolygon;
import org.mongodb.morphia.geo.Point;
import org.mongodb.morphia.geo.Polygon;

/**
 * Represents a document field in a query and presents the operations available to querying against that field.
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

    T lessThan(Object val);

    T lessThanOrEq(Object val);

    T in(Iterable<?> values);

    T mod(long divisor, long remainder);

    FieldEnd<T> not();

    T notEqual(Object val);

    T notIn(Iterable<?> values);

    T near(double x, double y);

    T near(double x, double y, boolean spherical);

    T near(double x, double y, double radius);

    T near(double x, double y, double radius, boolean spherical);

    T sizeEq(int val);

    T startsWith(String prefix);

    T startsWithIgnoreCase(final String prefix);

    /**
     * This implements the $geoWithin operator and is only compatible with mongo 2.4 or greater.
     */
    T within(Shape shape);

    T type(Type type);

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

    /**
     * This runs the $geoWithin query, returning documents with GeoJson fields
     * whose area falls within the given boundary. When determining
     * inclusion, MongoDB considers the border of a shape to be part of the
     * shape, subject to the precision of floating point numbers.
     *
     * These queries are only compatible with MongoDB 2.4 or greater.
     *
     * @param boundary a polygon describing the boundary to search within.
     * @return T
     */
    T within(Polygon boundary);

    /**
     * This runs the $geoWithin query, returning documents with GeoJson fields
     * whose area falls within the given boundaries. When determining
     * inclusion, MongoDB considers the border of a shape to be part of the
     * shape, subject to the precision of floating point numbers.
     *
     * These queries are only compatible with MongoDB 2.6 or greater.
     *
     * @param boundaries a multi-polygon describing the areas to search within.
     * @return T
     */
    T within(MultiPolygon boundaries);
}

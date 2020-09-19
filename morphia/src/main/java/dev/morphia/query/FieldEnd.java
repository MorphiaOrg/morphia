package dev.morphia.query;

import com.mongodb.client.model.geojson.MultiPolygon;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Polygon;
import dev.morphia.query.experimental.filters.Filter;
import dev.morphia.query.experimental.filters.Filters;

/**
 * Represents a document field in a query and presents the operations available to querying against that field.
 *
 * @param <T> the type of the FieldEnd
 */
@SuppressWarnings("removal")
@Deprecated(since = "2.0", forRemoval = true)
public interface FieldEnd<T> {

    /**
     * Checks if a field contains a value
     *
     * @param string the value to check for
     * @return T
     * @mongodb.driver.manual reference/operator/query/regex/ $regex
     * @deprecated use {@link Filters#regex(String)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T contains(String string);

    /**
     * Checks if a field contains a value ignoring the case of the values
     *
     * @param string the value to check for
     * @return T
     * @mongodb.driver.manual reference/operator/query/regex/ $regex
     * @deprecated use {@link Filters#regex(String)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T containsIgnoreCase(String string);

    /**
     * Checks that a field does not exist in a document
     *
     * @return T
     * @mongodb.driver.manual reference/operator/query/exists/ $exists
     * @deprecated use {@link Filters#exists(String)} with {@link Filter#not()} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T doesNotExist();

    /**
     * Checks that a field ends with a value
     *
     * @param suffix the value to check
     * @return T
     * @mongodb.driver.manual reference/operator/query/regex/ $regex
     * @deprecated use {@link Filters#regex(String)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T endsWith(String suffix);

    /**
     * Checks that a field ends with a value ignoring the case of the values
     *
     * @param suffix the value to check
     * @return T
     * @mongodb.driver.manual reference/operator/query/regex/ $regex
     * @deprecated use {@link Filters#regex(String)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T endsWithIgnoreCase(String suffix);

    /**
     * Checks that a field equals a value
     *
     * @param val the value to check
     * @return T
     * @mongodb.driver.manual reference/operator/query/eq/ $eq
     * @deprecated use {@link Filters#eq(String, Object)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T equal(Object val);

    /**
     * Checks that a field equals a value
     *
     * @param val the value to check
     * @return T
     * @mongodb.driver.manual reference/operator/query/eq/ $eq
     * @deprecated use {@link Filters#eq(String, Object)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T equalIgnoreCase(Object val);

    /**
     * Checks that a field matches the provided query definition
     *
     * @param query the query to find certain field values
     * @return T
     * @mongodb.driver.manual reference/operator/query/elemMatch/ $elemMatch
     * @deprecated use {@link Filters#elemMatch(String, Filter...)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T elemMatch(Query<?> query);

    /**
     * Checks that a field is greater than the value given
     *
     * @param val the value to check against
     * @return T
     * @mongodb.driver.manual reference/operator/query/gt/ $gt
     * @deprecated use {@link Filters#gt(String, Object)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T greaterThan(Object val);

    /**
     * Checks that a field is greater than or equal to the value given
     *
     * @param val the value to check against
     * @return T
     * @mongodb.driver.manual reference/operator/query/gte/ $gte
     * @deprecated use {@link Filters#gte(String, Object)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T greaterThanOrEq(Object val);

    /**
     * Checks that a field has all of the values listed.
     *
     * @param values the values to check against
     * @return T
     * @mongodb.driver.manual reference/operator/query/all/ $all
     * @deprecated use {@link Filters#all(String, Object)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T hasAllOf(Iterable<?> values);

    /**
     * Checks that a field has any of the values listed.
     *
     * @param values the values to check against
     * @return T
     * @mongodb.driver.manual reference/operator/query/in/ $in
     * @deprecated use {@link Filters#in(String, Iterable)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T hasAnyOf(Iterable<?> values);

    /**
     * Checks that a field has none of the values listed.
     *
     * @param values the values to check against
     * @return T
     * @mongodb.driver.manual reference/operator/query/nin/ $nin
     * @deprecated use {@link Filters#nin(String, Object)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T hasNoneOf(Iterable<?> values);

    /**
     * Checks that a field exists in a document
     *
     * @return T
     * @mongodb.driver.manual reference/operator/query/exists/ $exists
     * @deprecated use {@link Filters#exists(String)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T exists();

    /**
     * Checks that a field has the value listed.
     *
     * @param val the value to check against
     * @return T
     * @mongodb.driver.manual reference/operator/query/eq/ $eq
     * @deprecated use {@link Filters#eq(String, Object)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T hasThisOne(Object val);

    /**
     * Synonym for {@link #hasAnyOf(Iterable)}
     *
     * @param values the values to check against
     * @return T
     * @mongodb.driver.manual reference/operator/query/in/ $in
     * @deprecated use {@link Filters#in(String, Iterable)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T in(Iterable<?> values);

    /**
     * This performs a $geoIntersects query, searching documents containing any sort of GeoJson field and returning those where the given
     * geometry intersects with the document shape.  This includes cases where the data and the specified object share an edge.
     *
     * @param geometry the shape to use to query for any stored shapes that intersect
     * @return T
     * @mongodb.driver.manual reference/operator/query/geoIntersects/ $geoIntersects
     * @mongodb.server.release 2.4
     * @deprecated use {@link Filters#geoIntersects(String, com.mongodb.client.model.geojson.Geometry)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default T intersects(dev.morphia.geo.Geometry geometry) {
        return intersects(geometry.convert());
    }

    /**
     * This performs a $geoIntersects query, searching documents containing any sort of GeoJson field and returning those where the given
     * geometry intersects with the document shape.  This includes cases where the data and the specified object share an edge.
     *
     * @param geometry the shape to use to query for any stored shapes that intersect
     * @return T
     * @mongodb.driver.manual reference/operator/query/geoIntersects/ $geoIntersects
     * @mongodb.server.release 2.4
     * @since 2.0
     * @deprecated use {@link Filters#geoIntersects(String, com.mongodb.client.model.geojson.Geometry)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T intersects(com.mongodb.client.model.geojson.Geometry geometry);

    /**
     * This performs a $geoIntersects query, searching documents containing any sort of GeoJson field and returning those where the given
     * geometry intersects with the document shape.  This includes cases where the data and the specified object share an edge.
     *
     * @param geometry the shape to use to query for any stored shapes that intersect
     * @param crs      the coordinate reference system to use with the query
     * @return T
     * @mongodb.driver.manual reference/operator/query/geometry $geometry
     * @deprecated use {@link Filters#geometry(String, Object)} instead
     * @mongodb.server.release 2.4
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default T intersects(dev.morphia.geo.Geometry geometry, dev.morphia.geo.CoordinateReferenceSystem crs) {
        return intersects(geometry.convert(), crs.convert());
    }

    /**
     * This performs a $geoIntersects query, searching documents containing any sort of GeoJson field and returning those where the given
     * geometry intersects with the document shape.  This includes cases where the data and the specified object share an edge.
     *
     * @param geometry the shape to use to query for any stored shapes that intersect
     * @param crs      the coordinate reference system to use with the query
     * @return T
     * @mongodb.driver.manual reference/operator/query/geometry $geometry
     * @deprecated use {@link Filters#geometry(String, Object)} instead
     * @mongodb.server.release 2.4
     * @since 2.0
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T intersects(com.mongodb.client.model.geojson.Geometry geometry, com.mongodb.client.model.geojson.CoordinateReferenceSystem crs);

    /**
     * Checks that a field is less than the value given
     *
     * @param val the value to check against
     * @return T
     * @mongodb.driver.manual reference/operator/query/lt/ $lt
     * @deprecated use {@link Filters#lt(String, Object)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T lessThan(Object val);

    /**
     * Checks that a field is less than or equal to the value given
     *
     * @param val the value to check against
     * @return T
     * @mongodb.driver.manual reference/operator/query/lte/ $lte
     * @deprecated use {@link Filters#lte(String, Object)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T lessThanOrEq(Object val);

    /**
     * Select documents where the value of a field divided by a divisor has the specified remainder (i.e. perform a modulo operation
     * to select documents)
     *
     * @param divisor   the divisor to apply
     * @param remainder the remainder to check for
     * @return T
     * @mongodb.driver.manual reference/operator/query/mod/ $mod
     * @deprecated use {@link Filters#mod(String, long, long)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T mod(long divisor, long remainder);

    /**
     * Specifies a point for which a geospatial query returns the documents from nearest to farthest.
     *
     * @param longitude the longitude
     * @param latitude  the latitude
     * @return T
     * @mongodb.driver.manual reference/operator/query/near/ $near
     * @deprecated use {@link Filters#near(String, com.mongodb.client.model.geojson.Point)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T near(double longitude, double latitude);

    /**
     * Specifies a point for which a geospatial query returns the documents from nearest to farthest.
     *
     * @param longitude the longitude
     * @param latitude  the latitude
     * @param spherical if true, will use spherical geometry ($nearSphere) when analyzing documents
     * @return T
     * @mongodb.driver.manual reference/operator/query/near/ $near
     * @mongodb.driver.manual reference/operator/query/nearSphere/ $nearSphere
     * @deprecated use {@link Filters#nearSphere(String, com.mongodb.client.model.geojson.Point)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T near(double longitude, double latitude, boolean spherical);

    /**
     * Specifies a point for which a geospatial query returns the documents from nearest to farthest.
     *
     * @param longitude the longitude
     * @param latitude  the latitude
     * @param radius    the max distance to consider
     * @return T
     * @mongodb.driver.manual reference/operator/query/near/ $near
     * @deprecated use {@link Filters#near(String, com.mongodb.client.model.geojson.Point)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T near(double longitude, double latitude, double radius);

    /**
     * Specifies a point for which a geospatial query returns the documents from nearest to farthest.
     *
     * @param longitude the longitude
     * @param latitude  the latitude
     * @param radius    the max distance to consider
     * @param spherical if true, will use spherical geometry ($nearSphere) when analyzing documents
     * @return T
     * @mongodb.driver.manual reference/operator/query/near/ $near
     * @mongodb.driver.manual reference/operator/query/nearSphere/ $nearSphere
     * @deprecated use {@link Filters#nearSphere(String, com.mongodb.client.model.geojson.Point)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T near(double longitude, double latitude, double radius, boolean spherical);

    /**
     * This runs a $near query to check for documents geographically close to the given Point - this Point represents a GeoJSON point type.
     * These queries are only supported by MongoDB version 2.4 or greater.
     *
     * @param point the point to find results close to
     * @return T
     * @mongodb.driver.manual reference/operator/query/near/ $near
     * @deprecated use {@link Filters#near(String, com.mongodb.client.model.geojson.Point)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default T near(dev.morphia.geo.Point point) {
        return near(point.convert());
    }

    /**
     * This runs a $near query to check for documents geographically close to the given Point - this Point represents a GeoJSON point type.
     * These queries are only supported by MongoDB version 2.4 or greater.
     *
     * @param point the point to find results close to
     * @return T
     * @mongodb.driver.manual reference/operator/query/near/ $near
     * @since 2.0
     * @deprecated use {@link Filters#near(String, com.mongodb.client.model.geojson.Point)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T near(com.mongodb.client.model.geojson.Point point);

    /**
     * Specifies a point for which a geospatial query returns the documents from nearest to farthest.
     *
     * @param point       the point to find results close to
     * @param maxDistance the maximum distance in meters from the point
     * @param minDistance the minimum distance in meters from the point
     * @return T
     * @mongodb.driver.manual reference/operator/query/near/ $near
     * @since 1.5
     * @deprecated use {@link Filters#near(String, com.mongodb.client.model.geojson.Point)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default T near(dev.morphia.geo.Point point, Double maxDistance, Double minDistance) {
        return near(point.convert(), maxDistance, minDistance);
    }

    /**
     * Specifies a point for which a geospatial query returns the documents from nearest to farthest.
     *
     * @param point       the point to find results close to
     * @param maxDistance the maximum distance in meters from the point
     * @param minDistance the minimum distance in meters from the point
     * @return T
     * @mongodb.driver.manual reference/operator/query/near/ $near
     * @since 2.0
     * @deprecated use {@link Filters#near(String, com.mongodb.client.model.geojson.Point)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T near(com.mongodb.client.model.geojson.Point point, Double maxDistance, Double minDistance);

    /**
     * Specifies a point for which a geospatial query returns the documents from nearest to farthest.
     *
     * @param point the point to find results close to
     * @return T
     * @mongodb.driver.manual reference/operator/query/nearSphere/ $nearSphere
     * @since 1.5
     * @deprecated use {@link Filters#nearSphere(String, com.mongodb.client.model.geojson.Point)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default T nearSphere(dev.morphia.geo.Point point) {
        return nearSphere(point.convert());
    }

    /**
     * Specifies a point for which a geospatial query returns the documents from nearest to farthest.
     *
     * @param point the point to find results close to
     * @return T
     * @mongodb.driver.manual reference/operator/query/nearSphere/ $nearSphere
     * @since 2.0
     * @deprecated use {@link Filters#nearSphere(String, com.mongodb.client.model.geojson.Point)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T nearSphere(com.mongodb.client.model.geojson.Point point);

    /**
     * Specifies a point for which a geospatial query returns the documents from nearest to farthest.
     *
     * @param point       the point to find results close to
     * @param maxDistance the maximum distance in meters from the point
     * @param minDistance the minimum distance in meters from the point
     * @return T
     * @mongodb.driver.manual reference/operator/query/nearSphere/ $nearSphere
     * @since 1.5
     * @deprecated use {@link Filters#nearSphere(String, com.mongodb.client.model.geojson.Point)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default T nearSphere(dev.morphia.geo.Point point, Double maxDistance, Double minDistance) {
        return nearSphere(point.convert(), maxDistance, minDistance);
    }

    /**
     * Specifies a point for which a geospatial query returns the documents from nearest to farthest.
     *
     * @param point       the point to find results close to
     * @param maxDistance the maximum distance in meters from the point
     * @param minDistance the minimum distance in meters from the point
     * @return T
     * @mongodb.driver.manual reference/operator/query/nearSphere/ $nearSphere
     * @since 2.0
     * @deprecated use {@link Filters#nearSphere(String, com.mongodb.client.model.geojson.Point)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T nearSphere(com.mongodb.client.model.geojson.Point point, Double maxDistance, Double minDistance);

    /**
     * Negates the criteria applied to the field
     *
     * @return this
     */
    @Deprecated(since = "2.0", forRemoval = true)
    FieldEnd<T> not();

    /**
     * Checks that a field doesn't equal a value
     *
     * @param val the value to check
     * @return T
     * @mongodb.driver.manual reference/operator/query/ne/ $ne
     * @deprecated use {@link Filters#ne(String, Object)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T notEqual(Object val);

    /**
     * Synonym for {@link #hasNoneOf(Iterable)}
     *
     * @param values the values to check against
     * @return T
     * @mongodb.driver.manual reference/operator/query/nin/ $nin
     * @deprecated use {@link Filters#nin(String, Object)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T notIn(Iterable<?> values);

    /**
     * Checks the size of a field.
     *
     * @param val the value to check against
     * @return T
     * @mongodb.driver.manual reference/operator/query/size/ $size
     * @deprecated use {@link Filters#size(String, int)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T sizeEq(int val);

    /**
     * Checks that a field starts with a value
     *
     * @param prefix the value to check
     * @return T
     * @mongodb.driver.manual reference/operator/query/regex/ $regex
     * @deprecated use {@link Filters#regex(String)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T startsWith(String prefix);

    /**
     * Checks that a field starts with a value ignoring the case of the values
     *
     * @param prefix the value to check
     * @return T
     * @mongodb.driver.manual reference/operator/query/regex/ $regex
     * @deprecated use {@link Filters#regex(String)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T startsWithIgnoreCase(String prefix);

    /**
     * Checks the type of a field
     *
     * @param type the value to check against
     * @return T
     * @deprecated use {@link Filters#type(String, Type)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T type(Type type);

    /**
     * This implements the $geoWithin operator and is only compatible with mongo 2.4 or greater.
     *
     * @param shape the shape to check within
     * @return T
     * @mongodb.driver.manual reference/operator/query/geoWithin/ $geoWithin
     * @mongodb.server.release 2.4
     * @deprecated use {@link Filters#geoWithin(String, Polygon)}, {@link Filters#geoWithin(String, MultiPolygon)},
     * {@link Filters#box(String, Point, Point)}, {@link Filters#center(String, Point, double)},
     * {@link Filters#centerSphere(String, Point, double)}, or {@link Filters#polygon(String, Point[])} instead
     * instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T within(Shape shape);

    /**
     * This runs the $geoWithin query, returning documents with GeoJson fields whose area falls within the given boundary. When determining
     * inclusion, MongoDB considers the border of a shape to be part of the shape, subject to the precision of floating point numbers.
     *
     * @param boundary a polygon describing the boundary to search within.
     * @return T
     * @mongodb.driver.manual reference/operator/query/geoWithin/ $geoWithin
     * @mongodb.server.release 2.4
     * @deprecated use {@link Filters#geoWithin(String, Polygon)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default T within(dev.morphia.geo.Polygon boundary) {
        return within(boundary.convert());
    }

    /**
     * This runs the $geoWithin query, returning documents with GeoJson fields whose area falls within the given boundary. When determining
     * inclusion, MongoDB considers the border of a shape to be part of the shape, subject to the precision of floating point numbers.
     *
     * @param boundary a polygon describing the boundary to search within.
     * @return T
     * @mongodb.driver.manual reference/operator/query/geoWithin/ $geoWithin
     * @mongodb.server.release 2.4
     * @since 2.0
     * @deprecated use {@link Filters#geoWithin(String, Polygon)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T within(com.mongodb.client.model.geojson.Polygon boundary);

    /**
     * This runs the $geoWithin query, returning documents with GeoJson fields whose area falls within the given boundaries. When
     * determining inclusion, MongoDB considers the border of a shape to be part of the shape, subject to the precision of floating point
     * numbers.
     *
     * @param boundaries a multi-polygon describing the areas to search within.
     * @return T
     * @mongodb.driver.manual reference/operator/query/geoWithin/ $geoWithin
     * @mongodb.server.release 2.6
     * @deprecated use {@link Filters#geoWithin(String, MultiPolygon)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default T within(dev.morphia.geo.MultiPolygon boundaries) {
        return within(boundaries.convert());
    }

    /**
     * This runs the $geoWithin query, returning documents with GeoJson fields whose area falls within the given boundaries. When
     * determining inclusion, MongoDB considers the border of a shape to be part of the shape, subject to the precision of floating point
     * numbers.
     *
     * @param boundaries a multi-polygon describing the areas to search within.
     * @return T
     * @mongodb.driver.manual reference/operator/query/geoWithin/ $geoWithin
     * @mongodb.server.release 2.6
     * @since 2.0
     * @deprecated use {@link Filters#geoWithin(String, MultiPolygon)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T within(com.mongodb.client.model.geojson.MultiPolygon boundaries);

    /**
     * This runs the $geoWithin query, returning documents with GeoJson fields whose area falls within the given boundary. When determining
     * inclusion, MongoDB considers the border of a shape to be part of the shape, subject to the precision of floating point numbers.
     *
     * @param boundary a polygon describing the boundary to search within.
     * @param crs      the coordinate reference system to use
     * @return T
     * @mongodb.driver.manual reference/operator/query/geoWithin/ $geoWithin
     * @mongodb.server.release 2.4
     * @deprecated use {@link Filters#geoWithin(String, Polygon)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default T within(dev.morphia.geo.Polygon boundary, dev.morphia.geo.CoordinateReferenceSystem crs) {
        return within(boundary.convert(), crs.convert());
    }

    /**
     * This runs the $geoWithin query, returning documents with GeoJson fields whose area falls within the given boundary. When determining
     * inclusion, MongoDB considers the border of a shape to be part of the shape, subject to the precision of floating point numbers.
     *
     * @param boundary a polygon describing the boundary to search within.
     * @param crs      the coordinate reference system to use
     * @return T
     * @mongodb.driver.manual reference/operator/query/geoWithin/ $geoWithin
     * @mongodb.server.release 2.4
     * @since 2.0
     * @deprecated use {@link Filters#geoWithin(String, Polygon)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T within(com.mongodb.client.model.geojson.Polygon boundary, com.mongodb.client.model.geojson.CoordinateReferenceSystem crs);

    /**
     * This runs the $geoWithin query, returning documents with GeoJson fields whose area falls within the given boundaries. When
     * determining inclusion, MongoDB considers the border of a shape to be part of the shape, subject to the precision of floating point
     * numbers.
     * <p/>
     * These queries are only compatible with MongoDB 2.6 or greater.
     *
     * @param boundaries a multi-polygon describing the areas to search within.
     * @param crs        the coordinate reference system to use
     * @return T
     * @mongodb.driver.manual reference/operator/query/geoWithin/ $geoWithin
     * @deprecated use {@link Filters#geoWithin(String, MultiPolygon)} instead
     * @mongodb.server.release 2.6
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default T within(dev.morphia.geo.MultiPolygon boundaries, dev.morphia.geo.CoordinateReferenceSystem crs) {
        return within(boundaries.convert(), crs.convert());
    }

    /**
     * This runs the $geoWithin query, returning documents with GeoJson fields whose area falls within the given boundaries. When
     * determining inclusion, MongoDB considers the border of a shape to be part of the shape, subject to the precision of floating point
     * numbers.
     * <p/>
     * These queries are only compatible with MongoDB 2.6 or greater.
     *
     * @param boundaries a multi-polygon describing the areas to search within.
     * @param crs      the coordinate reference system to use
     * @return T
     * @mongodb.driver.manual reference/operator/query/geoWithin/ $geoWithin
     * @deprecated use {@link Filters#geoWithin(String, MultiPolygon)} instead
     * @mongodb.server.release 2.6
     * @since 2.0
     */
    @Deprecated(since = "2.0", forRemoval = true)
    T within(com.mongodb.client.model.geojson.MultiPolygon boundaries, com.mongodb.client.model.geojson.CoordinateReferenceSystem crs);
}

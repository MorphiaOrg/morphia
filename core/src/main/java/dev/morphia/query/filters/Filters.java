package dev.morphia.query.filters;

import java.util.regex.Pattern;

import com.mongodb.client.model.geojson.Geometry;
import com.mongodb.client.model.geojson.MultiPolygon;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.lang.Nullable;

import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.query.Type;

import org.bson.Document;

import static java.util.Arrays.asList;

/**
 * Defines helper methods to generate filter operations for queries.
 *
 * @since 2.0
 */
@SuppressWarnings("unused")
public final class Filters {
    private Filters() {
    }

    /**
     * Matches arrays that contain all elements specified in the query.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $all
     */
    public static Filter all(String field, @Nullable Object val) {
        return new Filter("$all", field, val);
    }

    /**
     * Applies $and to a set of filters
     *
     * @param filters the filters
     * @return the filter
     * @query.filter $and
     */
    public static Filter and(Filter... filters) {
        return new LogicalFilter("$and", filters);
    }

    /**
     * Matches numeric or binary values in which a set of bit positions all have a value of 0.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $bitsAllClear
     * @since 3.0 changed to take a plain Object instead of overloading
     */
    public static Filter bitsAllClear(String field, Object val) {
        return new Filter("$bitsAllClear", field, val);
    }

    /**
     * Matches numeric or binary values in which a set of bit positions all have a value of 1.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $bitsAllSet
     * @since 3.0 changed to take a plain Object instead of overloading
     */
    public static Filter bitsAllSet(String field, Object val) {
        return new Filter("$bitsAllSet", field, val);
    }

    /**
     * Matches numeric or binary values in which any bit from a set of bit positions has a value of 0.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $bitsAnyClear
     */
    public static Filter bitsAnyClear(String field, Object val) {
        return new Filter("$bitsAnyClear", field, val);
    }

    /**
     * Matches numeric or binary values in which any bit from a set of bit positions has a value of 1.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $bitsAnySet
     */
    public static Filter bitsAnySet(String field, Object val) {
        return new Filter("$bitsAnySet", field, val);
    }

    /**
     * Specifies a rectangular box using legacy coordinate pairs for $geoWithin queries. The 2d index supports $box.
     *
     * @param field      the field to check
     * @param bottomLeft the bottom left corner of the box
     * @param upperRight the upper right corner of the box
     * @return the filter
     * @query.filter $box
     */
    public static Filter box(String field, Point bottomLeft, Point upperRight) {
        return new Box(field, bottomLeft, upperRight);
    }

    /**
     * Specifies a circle using legacy coordinate pairs to $geoWithin queries when using planar geometry. The 2d index supports $center.
     *
     * @param field  the field to check
     * @param center the center point of the shape
     * @param radius the radius of the circle
     * @return the filter
     * @query.filter $center
     */
    public static Filter center(String field, Point center, double radius) {
        return new CenterFilter("$center", field, center, radius);
    }

    /**
     * Defines a circle for a geospatial query that uses spherical geometry. The query returns documents that are within the bounds of
     * the circle. You can use the $centerSphere operator on both GeoJSON objects and legacy coordinate pairs.
     * <p>
     * To use $centerSphere, specify an array that contains:
     *
     * <ol>
     * <li>The grid coordinates of the circle’s center point, and
     * <li>The circle’s radius measured in radians. To calculate radians, see Calculate Distance Using Spherical Geometry.
     * </ol>
     *
     * @param field  the field to check
     * @param center the center point of the shape
     * @param radius the radius of the circle
     * @return the filter
     * @query.filter $centerSphere
     */
    public static Filter centerSphere(String field, Point center, double radius) {
        return new CenterFilter("$centerSphere", field, center, radius);
    }

    /**
     * Adds a comment to a query predicate.
     *
     * @param comment the comment to attach
     * @return the filter
     * @query.filter $comment
     */
    public static Filter comment(String comment) {
        return new CommentFilter(comment);
    }

    /**
     * Selects documents if a value in the results matches all the specified $elemMatch conditions.
     *
     * @param filters the filters to evaluate against
     * @return the filter
     * @query.filter $elemMatch
     */
    public static Filter elemMatch(Filter... filters) {
        return new ElemMatchFilter(asList(filters));
    }

    /**
     * Selects documents if element in the array field matches all the specified $elemMatch conditions.
     *
     * @param field   the field to check
     * @param filters the filters to evaluate against
     * @return the filter
     * @query.filter $elemMatch
     */
    public static Filter elemMatch(String field, Filter... filters) {
        return new ElemMatchFilter(field, asList(filters));
    }

    /**
     * Specifies equality condition. The $eq operator matches documents where the value of a field equals the specified value.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $eq
     */
    public static Filter eq(String field, @Nullable Object val) {
        return new EqFilter(field, val);
    }

    /**
     * Matches documents that have the specified field.
     *
     * @param field the field to check
     * @return the filter
     * @query.filter $exists
     */
    public static Filter exists(String field) {
        return new ExistsFilter(field);
    }

    /**
     * Allows use of aggregation expressions within the query language.
     *
     * @param expression the expression to evaluate
     * @return the filter
     * @query.filter $expr
     */
    public static Filter expr(Expression expression) {
        return new ExprFilter(expression);
    }

    /**
     * Selects geometries that intersect with a GeoJSON geometry. The 2dsphere index supports $geoIntersects.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $geoIntersects
     */
    public static Filter geoIntersects(String field, Geometry val) {
        return new GeoIntersectsFilter(field, val);
    }

    /**
     * Selects geometries within a bounding GeoJSON geometry. The 2dsphere and 2d indexes support $geoWithin.
     *
     * @param field   the field to check
     * @param polygon the polygon to check
     * @return the filter
     * @query.filter $geoWithin
     */
    public static GeoWithinFilter geoWithin(String field, Polygon polygon) {
        return new GeoWithinFilter(field, polygon);
    }

    /**
     * Selects geometries within a bounding GeoJSON geometry. The 2dsphere and 2d indexes support $geoWithin.
     *
     * @param field   the field to check
     * @param polygon the polygon to check
     * @return the filter
     * @query.filter $geoWithin
     */
    public static GeoWithinFilter geoWithin(String field, MultiPolygon polygon) {
        return new GeoWithinFilter(field, polygon);
    }

    /**
     * Specifies a geometry in GeoJSON format to geospatial query operators.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $geometry
     */
    public static Filter geometry(String field, Object val) {
        return new Filter("$geometry", field, val);
    }

    /**
     * $gt selects those documents where the value of the field is greater than the specified value.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $gt
     */
    public static Filter gt(String field, Object val) {
        return new Filter("$gt", field, val);
    }

    /**
     * $gte selects the documents where the value of the field is greater than or equal to a specified value
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $gte
     */
    public static Filter gte(String field, Object val) {
        return new Filter("$gte", field, val);
    }

    /**
     * $gte selects the documents where the value of the target field is greater than or equal to a specified value
     *
     * @param val the value to check
     * @return the filter
     * @query.filter $gte
     * @since 3.0
     */
    public static Filter gte(Object val) {
        return new FieldLessFilter("$gte", val);
    }

    /**
     * The $in operator selects the documents where the value of a field equals any value in the specified array.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $in
     */
    public static Filter in(String field, Iterable<?> val) {
        return new Filter("$in", field, val);
    }

    /**
     * Filters documents against the given JSON Schema.
     *
     * @param schema the schema to use
     * @return the filter
     * @query.filter $jsonSchema
     * @since 2.1
     */
    public static Filter jsonSchema(Document schema) {
        return new JsonSchemaFilter(schema);
    }

    /**
     * $lt selects the documents where the value of the field is less than the specified value.
     *
     * @param val the value to check
     * @return the filter
     * @query.filter $lt
     * @since 3.0
     */
    public static Filter lt(Object val) {
        return new FieldLessFilter("$lt", val);
    }

    /**
     * $lt selects the documents where the value of the field is less than the specified value.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $lt
     */
    public static Filter lt(String field, Object val) {
        return new Filter("$lt", field, val);
    }

    /**
     * $lte selects the documents where the value of the field is less than or equal to the specified value.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $lte
     */
    public static Filter lte(String field, Object val) {
        return new Filter("$lte", field, val);
    }

    /**
     * Specifies a maximum distance to limit the results of $near and $nearSphere queries. The 2dsphere and 2d indexes support $maxDistance.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $maxDistance
     */
    public static Filter maxDistance(String field, Object val) {
        return new Filter("$maxDistance", field, val);
    }

    /**
     * Specifies a minimum distance to limit the results of $near and $nearSphere queries. For use with 2dsphere index only.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $minDistance
     */
    public static Filter minDistance(String field, Object val) {
        return new Filter("$minDistance", field, val);
    }

    /**
     * Performs a modulo operation on the value of a field and selects documents with a specified result.
     *
     * @param field     the field to check
     * @param divisor   the value to divide by
     * @param remainder the remainder to check for
     * @return the filter
     * @query.filter $mod
     */
    public static Filter mod(String field, long divisor, long remainder) {
        return new ModFilter(field, divisor, remainder);
    }

    /**
     * Performs a modulo operation on the value of a field and selects documents with a specified result.
     *
     * @param field     the field to check
     * @param divisor   the value to divide by
     * @param remainder the remainder to check for
     * @return the filter
     * @query.filter $mod
     * @since 3.0
     */
    public static Filter mod(String field, double divisor, double remainder) {
        return new ModFilter(field, divisor, remainder);
    }

    /**
     * $ne selects the documents where the value of the field is not equal to the specified value. This includes documents that do not
     * contain the field.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $ne
     */
    public static Filter ne(String field, @Nullable Object val) {
        return new Filter("$ne", field, val);
    }

    /**
     * Specifies a point for which a geospatial query returns the documents from nearest to farthest. The $near operator can specify
     * either a GeoJSON point or legacy coordinate point.
     * <p>
     * This requires a geospatial index.
     *
     * @param field the field to check
     * @param point the point to check
     * @return the filter
     * @query.filter $near
     */
    public static NearFilter near(String field, Point point) {
        return new NearFilter("$near", field, point);
    }

    /**
     * Returns geospatial objects in proximity to a point on a sphere.
     * <p>
     * Requires a geospatial index. The 2dsphere and 2d indexes support
     * $nearSphere.
     *
     * @param field the field to check
     * @param point the point to check
     * @return the filter
     * @query.filter $nearSphere
     */
    public static NearFilter nearSphere(String field, Point point) {
        return new NearFilter("$nearSphere", field, point);
    }

    /**
     * $nin selects the documents where:
     *
     * <ul>
     * <li>the field value is not in the specified array or
     * <li>the field does not exist.
     * </ul>
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $nin
     */
    public static Filter nin(String field, Object val) {
        return new Filter("$nin", field, val);
    }

    /**
     * Applies $nor to a set of filters
     *
     * @param filters the filters
     * @return the filter
     * @query.filter $nor
     */
    public static Filter nor(Filter... filters) {
        return new LogicalFilter("$nor", filters);
    }

    /**
     * Applies $or to a set of filters
     *
     * @param filters the filters
     * @return the filter
     * @query.filter $or
     */
    public static Filter or(Filter... filters) {
        return new LogicalFilter("$or", filters);
    }

    /**
     * Specifies a polygon to using legacy coordinate pairs for $geoWithin queries. The 2d index supports $center.
     *
     * @param field  the field to check
     * @param points the value to check
     * @return the filter
     * @query.filter $polygon
     */
    public static Filter polygon(String field, Point... points) {
        return new PolygonFilter(field, points);
    }

    /**
     * Selects documents where values match a specified regular expression.
     *
     * @param field   the field to check
     * @param pattern the regex pattern
     * @return the filter
     * @query.filter $regex
     * @mongodb.server.release 1.9.0
     * @since 2.4.0
     */
    public static RegexFilter regex(String field, String pattern) {
        return new RegexFilter(field, Pattern.compile(pattern));
    }

    /**
     * Selects documents where values match a specified regular expression.
     *
     * @param field   the field to check
     * @param pattern the regex pattern
     * @return the filter
     * @query.filter $regex
     * @mongodb.server.release 1.9.0
     * @since 2.4.0
     */
    public static RegexFilter regex(String field, Pattern pattern) {
        return new RegexFilter(field, pattern);
    }

    /**
     * Selects documents if the array field is a specified size.
     *
     * @param field the field to check
     * @param size  the size to check against
     * @return the filter
     * @query.filter $size
     */
    public static Filter size(String field, int size) {
        return new Filter("$size", field, size);
    }

    /**
     * Performs text search.
     *
     * @param textSearch the text to search for
     * @return the filter
     * @query.filter $text
     */
    public static TextSearchFilter text(String textSearch) {
        return new TextSearchFilter(textSearch);
    }

    /**
     * Selects documents if a field is of the specified type.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $type
     */
    public static Filter type(String field, Type... val) {
        return new Filter("$type", field, val.length == 1 ? val[0] : val);
    }

    /**
     * Deprecated. Modifies a $geoWithin and $near queries to ensure that even if a document matches the query multiple times, the query
     * returns the document once.}
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $uniqueDocs
     */
    public static Filter uniqueDocs(String field, Object val) {
        return new Filter("$uniqueDocs", field, val);
    }

    /**
     * Matches documents that satisfy a JavaScript expression.
     *
     * @param val the value to check
     * @return the filter
     * @query.filter $where
     */
    public static Filter where(String val) {
        return new WhereFilter(val);
    }

}

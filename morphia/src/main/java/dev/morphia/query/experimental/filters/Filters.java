package dev.morphia.query.experimental.filters;

import com.mongodb.client.model.geojson.Geometry;
import com.mongodb.client.model.geojson.Point;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.Type;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static java.lang.String.format;
import static java.util.Arrays.asList;

/**
 * Defines helper methods to generate filter operations for queries.
 *
 * @since 2.0
 */
public final class Filters {
    private Filters() {
    }

    /**
     * Specifies equality condition. The $eq operator matches documents where the value of a field equals the specified value.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $eq
     */
    public static Filter eq(final String field, final Object val) {
        return new Filter("$eq", field, val) {
            @Override
            public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext context) {
                writer.writeName(field(mapper));
                writeUnnamedValue(getValue(mapper), mapper, writer, context);
            }
        };
    }

    /**
     * Matches documents that have the specified field.
     *
     * @param field the field to check
     * @return the filter
     * @query.filter $exists
     */
    public static Filter exists(final String field) {
        return new Filter("$exists", field, null) {
            @Override
            public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext context) {
                writer.writeStartDocument(field(mapper));
                writer.writeName(getFilterName());
                writer.writeBoolean(!isNot());
                writer.writeEndDocument();
            }
        };
    }

    /**
     * Selects documents if a field is of the specified type.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $type
     */
    public static Filter type(final String field, final Type val) {
        return new Filter("$type", field, val.toString().toLowerCase());
    }

    /**
     * Allows use of aggregation expressions within the query language.
     *
     * @param expression the expression to evaluate
     * @return the filter
     * @query.filter $expr
     */
    public static Filter expr(final Expression expression) {
        return new Filter("$expr", null, expression) {
            @Override
            public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext context) {
                writer.writeName("$expr");
                getValue().encode(mapper, writer, context);
            }

            @Override
            protected Expression getValue() {
                return (Expression) super.getValue();
            }
        };
    }

    /**
     * Validate documents against the given JSON Schema.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $jsonSchema
     */
    public static Filter jsonSchema(final String field, final Object val) {
        return new Filter("$jsonSchema", field, val);
    }

    /**
     * Performs a modulo operation on the value of a field and selects documents with a specified result.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $mod
     */
    public static Filter mod(final String field, final Object val) {
        return new Filter("$mod", field, val);
    }

    /**
     * Selects documents where values match a specified regular expression.
     *
     * @param field the field to check
     * @return the filter
     * @query.filter $regex
     */
    public static RegexFilter regex(final String field) {
        return new RegexFilter(field);
    }

    /**
     * Performs text search.
     *
     * @param textSearch the text to search for
     * @return the filter
     * @query.filter $text
     */
    public static TextSearchFilter text(final String textSearch) {
        return new TextSearchFilter(textSearch);
    }

    /**
     * Matches documents that satisfy a JavaScript expression.
     *
     * @param val the value to check
     * @return the filter
     * @query.filter $where
     */
    public static Filter where(final String val) {
        return new Filter("$where", null, val) {
            @Override
            public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext context) {
                writer.writeName(getFilterName());
                String value = getValue(mapper).toString().trim();
                if (!value.startsWith("function()")) {
                    value = format("function() { %s }", value);
                }
                writer.writeString(value);
            }
        };
    }

    /**
     * Selects geometries that intersect with a GeoJSON geometry. The 2dsphere index supports $geoIntersects.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $geoIntersects
     */
    public static Filter geoIntersects(final String field, final Geometry val) {
        return new GeoIntersectsFilter(field, val);
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
    public static NearFilter near(final String field, final Point point) {
        return new NearFilter("$near", field, point);
    }

    /**
     * Selects geometries within a bounding GeoJSON geometry. The 2dsphere and 2d indexes support $geoWithin.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $geoWithin
     */
    public static Filter geoWithin(final String field, final Geometry val) {
        return new Filter("$geoWithin", field, val);
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
    public static NearFilter nearSphere(final String field, final Point point) {
        return new NearFilter("$nearSphere", field, point);
    }

    /**
     * Matches arrays that contain all elements specified in the query.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $all
     */
    public static Filter all(final String field, final Object val) {
        return new Filter("$all", field, val);
    }

    /**
     * Selects documents if element in the array field matches all the specified $elemMatch conditions.
     *
     * @param field   the field to check
     * @param filters the filters to evaluate against
     * @return the filter
     * @query.filter $elemMatch
     */
    public static Filter elemMatch(final String field, final Filter... filters) {
        return new ElemMatchFilter(field, asList(filters));
    }

    /**
     * Selects documents if the array field is a specified size.
     *
     * @param field the field to check
     * @param size  the size to check against
     * @return the filter
     * @query.filter $size
     */
    public static Filter size(final String field, final int size) {
        return new Filter("$size", field, size);
    }

    /**
     * Matches numeric or binary values in which a set of bit positions all have a value of 0.
     *
     * @param field     the field to check
     * @param positions the value to check
     * @return the filter
     * @query.filter $bitsAllClear
     */
    public static Filter bitsAllClear(final String field, final int[] positions) {
        return new Filter("$bitsAllClear", field, positions);
    }

    /**
     * Matches numeric or binary values in which a set of bit positions all have a value of 0.
     *
     * @param field   the field to check
     * @param bitMask the numeric bitmask to use
     * @return the filter
     * @query.filter $bitsAllClear
     */
    public static Filter bitsAllClear(final String field, final int bitMask) {
        return new Filter("$bitsAllClear", field, bitMask);
    }

    /**
     * Matches numeric or binary values in which a set of bit positions all have a value of 1.
     *
     * @param field   the field to check
     * @param bitMask the numeric bitmask to use
     * @return the filter
     * @query.filter $bitsAllSet
     */
    public static Filter bitsAllSet(final String field, final int bitMask) {
        return new Filter("$bitsAllSet", field, bitMask);
    }

    /**
     * Matches numeric or binary values in which a set of bit positions all have a value of 1.
     *
     * @param field     the field to check
     * @param positions the value to check
     * @return the filter
     * @query.filter $bitsAllSet
     */
    public static Filter bitsAllSet(final String field, final int[] positions) {
        return new Filter("$bitsAllSet", field, positions);
    }

    /**
     * Matches numeric or binary values in which any bit from a set of bit positions has a value of 0.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $bitsAnyClear
     */
    public static Filter bitsAnyClear(final String field, final Object val) {
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
    public static Filter bitsAnySet(final String field, final Object val) {
        return new Filter("$bitsAnySet", field, val);
    }

    /**
     * Adds a comment to a query predicate.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $comment
     */
    public static Filter comment(final String field, final Object val) {
        return new Filter("$comment", field, val);
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
    public static Filter box(final String field, final Point bottomLeft, final Point upperRight) {
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
    public static Filter center(final String field, final Point center, final double radius) {
        return new CenterFilter("$center", field, center, radius);
    }

    /**
     * Defines a circle for a geospatial query that uses spherical geometry. The query returns documents that are within the bounds of
     * the circle. You can use the $centerSphere operator on both GeoJSON objects and legacy coordinate pairs.
     * <p>
     * To use $centerSphere, specify an array that contains:
     *
     * <li>The grid coordinates of the circle’s center point, and
     * <li>The circle’s radius measured in radians. To calculate radians, see Calculate Distance Using Spherical Geometry.
     *
     * @param field  the field to check
     * @param center the center point of the shape
     * @param radius the radius of the circle
     * @return the filter
     * @query.filter $centerSphere
     */
    public static Filter centerSphere(final String field, final Point center, final double radius) {
        return new CenterFilter("$centerSphere", field, center, radius);
    }

    /**
     * Specifies a geometry in GeoJSON format to geospatial query operators.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $geometry
     */
    public static Filter geometry(final String field, final Object val) {
        return new Filter("$geometry", field, val);
    }

    /**
     * Specifies a maximum distance to limit the results of $near and $nearSphere queries. The 2dsphere and 2d indexes support $maxDistance.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $maxDistance
     */
    public static Filter maxDistance(final String field, final Object val) {
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
    public static Filter minDistance(final String field, final Object val) {
        return new Filter("$minDistance", field, val);
    }

    /**
     * Specifies a polygon to using legacy coordinate pairs for $geoWithin queries. The 2d index supports $center.
     *
     * @param field  the field to check
     * @param points the value to check
     * @return the filter
     * @query.filter $polygon
     */
    public static Filter polygon(final String field, final Point[] points) {
        return new PolygonFilter(field, points);
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
    public static Filter uniqueDocs(final String field, final Object val) {
        return new Filter("$uniqueDocs", field, val);
    }

    /**
     * $gt selects those documents where the value of the field is greater than the specified value.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $gt
     */
    public static Filter gt(final String field, final Object val) {
        return new Filter("$gt", field, val);
    }

    /**
     * $gte selects the documents where the value of the field is greater than or equal to a specified value (e.g. value.)
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $gte
     */
    public static Filter gte(final String field, final Object val) {
        return new Filter("$gte", field, val);
    }

    /**
     * The $in operator selects the documents where the value of a field equals any value in the specified array.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $in
     */
    public static Filter in(final String field, final Object val) {
        return new Filter("$in", field, val);
    }

    /**
     * $lt selects the documents where the value of the field is less than the specified value.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $lt
     */
    public static Filter lt(final String field, final Object val) {
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
    public static Filter lte(final String field, final Object val) {
        return new Filter("$lte", field, val);
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
    public static Filter ne(final String field, final Object val) {
        return new Filter("$ne", field, val);
    }

    /**
     * $nin selects the documents where:
     *
     * <ul>
     *   <li>the field value is not in the specified array or
     *   <li>the field does not exist.
     * </ul>
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $nin
     */
    public static Filter nin(final String field, final Object val) {
        return new Filter("$nin", field, val);
    }

    /**
     * Applies $or to a set of filters
     *
     * @param filters the filters
     * @return the filter
     * @query.filter $or
     */
    public static Filter or(final Filter... filters) {
        return new LogicalFilter("$or", filters);
    }

    /**
     * Applies $and to a set of filters
     *
     * @param filters the filters
     * @return the filter
     * @query.filter $and
     */
    public static Filter and(final Filter... filters) {
        return new LogicalFilter("$and", filters);
    }

}



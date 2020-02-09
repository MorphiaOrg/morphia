package dev.morphia.query.experimental.filters;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

/**
 * Defines helper methods to generate filter operations for queries.
 *
 * @since 2.0
 */
public final class Filters {
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
                writeUnnamedValue(getValue(), mapper, writer, context);
            }
        };
    }

    /**
     * Matches documents that have the specified field.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $exists
     */
    public static Filter exists(final String field, final Object val) {
        return new Filter("$exists", field, val);
    }

    /**
     * Selects documents if a field is of the specified type.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $type
     */
    public static Filter type(final String field, final Object val) {
        return new Filter("$type", field, val);
    }

    /**
     * Allows use of aggregation expressions within the query language.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $expr
     */
    public static Filter expr(final String field, final Object val) {
        return new Filter("$expr", field, val);
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
        return new TextSearchFilter("$text", textSearch);
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
                String value = getValue().toString().trim();
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
    public static Filter geoIntersects(final String field, final Object val) {
        return new Filter("$geoIntersects", field, val);
    }

    /**
     * Returns geospatial objects in proximity to a point. Requires a geospatial index. The 2dsphere and 2d indexes support $near.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $near
     */
    public static Filter near(final String field, final Object val) {
        return new Filter("$near", field, val);
    }

    /**
     * Selects geometries within a bounding GeoJSON geometry. The 2dsphere and 2d indexes support $geoWithin.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $geoWithin
     */
    public static Filter geoWithin(final String field, final Object val) {
        return new Filter("$geoWithin", field, val);
    }

    /**
     * Returns geospatial objects in proximity to a point on a sphere. Requires a geospatial index. The 2dsphere and 2d indexes support
     * $nearSphere.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $nearSphere
     */
    public static Filter nearSphere(final String field, final Object val) {
        return new Filter("$nearSphere", field, val);
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
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $elemMatch
     */
    public static Filter elemMatch(final String field, final Object val) {
        return new Filter("$elemMatch", field, val);
    }

    /**
     * Selects documents if the array field is a specified size.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $size
     */
    public static Filter size(final String field, final Object val) {
        return new Filter("$size", field, val);
    }

    /**
     * Matches numeric or binary values in which a set of bit positions all have a value of 0.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $bitsAllClear
     */
    public static Filter bitsAllClear(final String field, final Object val) {
        return new Filter("$bitsAllClear", field, val);
    }

    /**
     * Matches numeric or binary values in which a set of bit positions all have a value of 1.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $bitsAllSet
     */
    public static Filter bitsAllSet(final String field, final Object val) {
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
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $box
     */
    public static Filter box(final String field, final Object val) {
        return new Filter("$box", field, val);
    }

    /**
     * Specifies a circle using legacy coordinate pairs to $geoWithin queries when using planar geometry. The 2d index supports $center.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $center
     */
    public static Filter center(final String field, final Object val) {
        return new Filter("$center", field, val);
    }

    /**
     * Specifies a circle using either legacy coordinate pairs or GeoJSON format for $geoWithin queries when using spherical geometry. The
     * 2dsphere and 2d indexes support $centerSphere.
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $centerSphere
     */
    public static Filter centerSphere(final String field, final Object val) {
        return new Filter("$centerSphere", field, val);
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
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $polygon
     */
    public static Filter polygon(final String field, final Object val) {
        return new Filter("$polygon", field, val);
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
     * $gt selects those documents where the value of the field is greater than (i.e. >) the specified value.
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
     * $gte selects the documents where the value of the field is greater than or equal to (i.e. >=) a specified value (e.g. value.)
     *
     * @param field the field to check
     * @param val   the value to check
     * @return the filter
     * @query.filter $gte
     */
    public static Filter gte(final String field, final Object val) {
        return new Filter("gte", field, val);
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
     * $lt selects the documents where the value of the field is less than (i.e. <) the specified value.
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
     * $lte selects the documents where the value of the field is less than or equal to (i.e. <=) the specified value.
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

    private static class LogicalFilter extends Filter {
        private final List<Filter> filters;

        public LogicalFilter(final String name, final Filter... filters) {
            super(name);
            this.filters = Arrays.asList(filters);
        }

        @Override
        public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext context) {
            writer.writeStartArray(getFilterName());
            for (final Filter filter : filters) {
                writer.writeStartDocument();
                filter.encode(mapper, writer, context);
                writer.writeEndDocument();
            }
            writer.writeEndArray();
        }

        @Override
        public Filter entityType(final Class<?> type) {
            super.entityType(type);
            for (final Filter filter : filters) {
                filter.entityType(type);
            }
            return this;
        }

        @Override
        public Filter isValidating(final boolean validate) {
            super.isValidating(validate);
            for (final Filter filter : filters) {
                filter.isValidating(validate);
            }
            return this;
        }

        @Override
        public String toString() {
            return format("%s: %s", getFilterName(), filters);
        }
    }
}



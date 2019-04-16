package dev.morphia.query;


import dev.morphia.geo.CoordinateReferenceSystem;
import dev.morphia.geo.Geometry;
import dev.morphia.geo.MultiPolygon;
import dev.morphia.geo.Point;
import dev.morphia.geo.Polygon;
import dev.morphia.utils.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static dev.morphia.query.FilterOperator.ALL;
import static dev.morphia.query.FilterOperator.ELEMENT_MATCH;
import static dev.morphia.query.FilterOperator.EQUAL;
import static dev.morphia.query.FilterOperator.EXISTS;
import static dev.morphia.query.FilterOperator.GEO_WITHIN;
import static dev.morphia.query.FilterOperator.GREATER_THAN;
import static dev.morphia.query.FilterOperator.GREATER_THAN_OR_EQUAL;
import static dev.morphia.query.FilterOperator.IN;
import static dev.morphia.query.FilterOperator.INTERSECTS;
import static dev.morphia.query.FilterOperator.LESS_THAN;
import static dev.morphia.query.FilterOperator.LESS_THAN_OR_EQUAL;
import static dev.morphia.query.FilterOperator.MOD;
import static dev.morphia.query.FilterOperator.NEAR;
import static dev.morphia.query.FilterOperator.NEAR_SPHERE;
import static dev.morphia.query.FilterOperator.NOT_EQUAL;
import static dev.morphia.query.FilterOperator.NOT_IN;
import static dev.morphia.query.FilterOperator.SIZE;
import static dev.morphia.query.FilterOperator.TYPE;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.quote;

/**
 * Represents a document field in a query and presents the operations available to querying against that field.  This is an internal
 * class and subject to change without notice.
 *
 * @param <T> the type of the CriteriaContainer
 */
public class FieldEndImpl<T extends CriteriaContainer> implements FieldEnd<T> {
    private static final Logger LOG = LoggerFactory.getLogger(FieldEndImpl.class);

    private final QueryImpl<?> query;
    private final String field;
    private final T target;
    private boolean not;

    /**
     * Creates a FieldEnd for a particular field.
     *
     * @param query  the owning query
     * @param field  the field to consider
     * @param target the CriteriaContainer
     */
    public FieldEndImpl(final QueryImpl<?> query, final String field, final T target) {
        this.query = query;
        this.field = field;
        this.target = target;
    }

    @Override
    public T contains(final String string) {
        Assert.parametersNotNull("val", string);
        return addCriteria(EQUAL, compile(quote(string)));
    }

    @Override
    public T containsIgnoreCase(final String string) {
        Assert.parametersNotNull("val", string);
        return addCriteria(EQUAL, compile(quote(string), CASE_INSENSITIVE));
    }

    @Override
    public T doesNotExist() {
        return addCriteria(EXISTS, false);
    }

    @Override
    public T endsWith(final String suffix) {
        Assert.parametersNotNull("val", suffix);
        return addCriteria(EQUAL, compile(quote(suffix) + "$"));
    }

    @Override
    public T endsWithIgnoreCase(final String suffix) {
        Assert.parametersNotNull("val", suffix);
        return addCriteria(EQUAL, compile(quote(suffix) + "$", CASE_INSENSITIVE));
    }

    @Override
    public T equal(final Object val) {
        return addCriteria(EQUAL, val);
    }

    @Override
    public T equalIgnoreCase(final Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(EQUAL, compile("^" + quote(val.toString()) + "$", CASE_INSENSITIVE));
    }

    @Override
    public T exists() {
        return addCriteria(EXISTS, true);
    }

    @Override
    public T greaterThan(final Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(GREATER_THAN, val);
    }

    @Override
    public T greaterThanOrEq(final Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(GREATER_THAN_OR_EQUAL, val);
    }

    @Override
    public T hasAllOf(final Iterable<?> values) {
        Assert.parametersNotNull("values", values);
        Assert.parameterNotEmpty("values", values);
        return addCriteria(ALL, values);
    }

    @Override
    public T hasAnyOf(final Iterable<?> values) {
        Assert.parametersNotNull("values", values);
        if (LOG.isWarnEnabled()) {
            if (!values.iterator().hasNext()) {
                LOG.warn("Specified an empty list/collection with the '" + field + "' criteria");
            }
        }
        return addCriteria(IN, values);
    }

    @Override
    public T hasNoneOf(final Iterable<?> values) {
        Assert.parametersNotNull("values", values);
        Assert.parameterNotEmpty("values", values);
        return addCriteria(NOT_IN, values);
    }

    @Override
    @Deprecated
    public T doesNotHaveThisElement(final Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(ELEMENT_MATCH, val, true);
    }

    @Override
    @Deprecated
    public T hasThisElement(final Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(ELEMENT_MATCH, val, not);
    }

    @Override
    public T elemMatch(final Query query) {
        Assert.parametersNotNull("query", query);
        return addCriteria(ELEMENT_MATCH, query, not);
    }

    @Override
    public T hasThisOne(final Object val) {
        return addCriteria(EQUAL, val);
    }

    @Override
    public T in(final Iterable<?> values) {
        return hasAnyOf(values);
    }

    @Override
    public T intersects(final Geometry geometry) {
        target.add(Geo2dSphereCriteria.geo(query, field, INTERSECTS, geometry));
        return target;
    }

    @Override
    public T intersects(final Geometry geometry, final CoordinateReferenceSystem crs) {
        target.add(Geo2dSphereCriteria.geo(query, field, INTERSECTS, geometry)
                                      .addCoordinateReferenceSystem(crs));
        return target;
    }

    @Override
    public T lessThan(final Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(LESS_THAN, val);
    }

    @Override
    public T lessThanOrEq(final Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(LESS_THAN_OR_EQUAL, val);
    }

    @Override
    public T mod(final long divisor, final long remainder) {
        return addCriteria(MOD, new long[]{divisor, remainder});
    }

    @Override
    public T near(final double longitude, final double latitude) {
        return near(longitude, latitude, false);
    }

    @Override
    public T near(final double longitude, final double latitude, final boolean spherical) {
        return addGeoCriteria(spherical ? NEAR_SPHERE : NEAR, new double[]{longitude, latitude}, null);
    }

    @Override
    public T near(final double longitude, final double latitude, final double radius) {
        return near(longitude, latitude, radius, false);
    }

    @Override
    public T near(final double longitude, final double latitude, final double radius, final boolean spherical) {
        return addGeoCriteria(spherical ? NEAR_SPHERE : NEAR,
            new double[]{longitude, latitude},
            opts("$maxDistance", radius));
    }

    @Override
    public T near(final Point point, final int maxDistance) {
        return near(point, (double) maxDistance, null);
    }

    @Override
    public T near(final Point point) {
        target.add(Geo2dSphereCriteria.geo(query, field, NEAR, point));
        return target;
    }

    @Override
    public T near(final Point point, final Double maxDistance, final Double minDistance) {
        target.add(Geo2dSphereCriteria.geo(query, field, NEAR, point)
                                      .maxDistance(maxDistance)
                                      .minDistance(minDistance));
        return target;
    }

    @Override
    public T nearSphere(final Point point) {
        return nearSphere(point, null, null);
    }

    @Override
    public T nearSphere(final Point point, final Double maxDistance, final Double minDistance) {
        target.add(Geo2dSphereCriteria.geo(query, field, NEAR_SPHERE, point)
                                      .maxDistance(maxDistance)
                                      .minDistance(minDistance));
        return target;
    }

    @Override
    public FieldEnd<T> not() {
        not = !not;
        return this;
    }

    @Override
    public T notEqual(final Object val) {
        return addCriteria(NOT_EQUAL, val);
    }

    @Override
    public T notIn(final Iterable<?> values) {
        return hasNoneOf(values);
    }

    @Override
    public T sizeEq(final int val) {
        return addCriteria(SIZE, val);
    }

    @Override
    public T startsWith(final String prefix) {
        Assert.parametersNotNull("val", prefix);
        /*LITERAL*/
        return addCriteria(EQUAL, compile("^" + quote(prefix)));
    }

    @Override
    public T startsWithIgnoreCase(final String prefix) {
        Assert.parametersNotNull("val", prefix);
        /*  | LITERAL */
        return addCriteria(EQUAL, compile("^" + quote(prefix), CASE_INSENSITIVE));
    }

    @Override
    public T type(final Type type) {
        return addCriteria(TYPE, type.val());
    }

    @Override
    public T within(final Shape shape) {
        Assert.parametersNotNull("shape", shape);
        return addCriteria(GEO_WITHIN, shape.toDBObject());
    }

    @Override
    public T within(final Polygon boundary) {
        target.add(Geo2dSphereCriteria.geo(query, field, GEO_WITHIN, boundary));
        return target;
    }

    @Override
    public T within(final MultiPolygon boundaries) {
        target.add(Geo2dSphereCriteria.geo(query, field, GEO_WITHIN, boundaries));
        return target;
    }

    @Override
    public T within(final Polygon boundary, final CoordinateReferenceSystem crs) {
        target.add(Geo2dSphereCriteria.geo(query, field, GEO_WITHIN, boundary)
                                      .addCoordinateReferenceSystem(crs));
        return target;
    }

    @Override
    public T within(final MultiPolygon boundaries, final CoordinateReferenceSystem crs) {
        target.add(Geo2dSphereCriteria.geo(query, field, GEO_WITHIN, boundaries)
                                      .addCoordinateReferenceSystem(crs));
        return target;
    }

    private T addCriteria(final FilterOperator op, final Object val) {
        return addCriteria(op, val, not);
    }

    private T addCriteria(final FilterOperator op, final Object val, final boolean not) {
        target.add(new FieldCriteria(query, field, op, val, not));
        return target;
    }

    private T addGeoCriteria(final FilterOperator op, final Object val, final Map<String, Object> opts) {
        if (not) {
            throw new QueryException("Geospatial queries cannot be negated with 'not'.");
        }

        target.add(new Geo2dCriteria(query, field, op, val, opts));
        return target;
    }

    private Map<String, Object> opts(final String s, final Object v) {
        final Map<String, Object> opts = new HashMap<String, Object>();
        opts.put(s, v);
        return opts;
    }
}

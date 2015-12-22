package org.mongodb.morphia.query;


import org.mongodb.morphia.geo.CoordinateReferenceSystem;
import org.mongodb.morphia.geo.Geometry;
import org.mongodb.morphia.geo.MultiPolygon;
import org.mongodb.morphia.geo.Point;
import org.mongodb.morphia.geo.Polygon;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.utils.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.mongodb.morphia.query.FilterOperator.GEO_WITHIN;
import static org.mongodb.morphia.query.FilterOperator.INTERSECTS;

/**
 * Represents a document field in a query and presents the operations available to querying against that field.
 *
 * @param <T> the type of the CriteriaContainer
 */
public class FieldEndImpl<T extends CriteriaContainerImpl> implements FieldEnd<T> {
    private static final Logger LOG = MorphiaLoggerFactory.get(FieldEndImpl.class);

    private final QueryImpl<?> query;
    private final String field;
    private final T target;
    private final boolean validateName;
    private boolean not;

    /**
     * Creates a FieldEnd for a particular field.
     *
     * @param query        the owning query
     * @param field        the field to consider
     * @param target       the CriteriaContainer
     * @param validateName true if the field name should be validated
     */
    public FieldEndImpl(final QueryImpl<?> query, final String field, final T target, final boolean validateName) {

        this(query, field, target, validateName, false);
    }

    private FieldEndImpl(final QueryImpl<?> query, final String field, final T target, final boolean validateName, final boolean not) {
        this.query = query;
        this.field = field;
        this.target = target;
        this.validateName = validateName;
        this.not = not;
    }

    @Override
    public T contains(final String string) {
        Assert.parametersNotNull("val", string);
        return addCriteria(FilterOperator.EQUAL, Pattern.compile(string));
    }

    @Override
    public T containsIgnoreCase(final String string) {
        Assert.parametersNotNull("val", string);
        return addCriteria(FilterOperator.EQUAL, Pattern.compile(string, Pattern.CASE_INSENSITIVE));
    }

    @Override
    public T doesNotExist() {
        return addCriteria(FilterOperator.EXISTS, false);
    }

    @Override
    public T endsWith(final String suffix) {
        Assert.parametersNotNull("val", suffix);
        return addCriteria(FilterOperator.EQUAL, Pattern.compile(suffix + "$"));
    }

    @Override
    public T endsWithIgnoreCase(final String suffix) {
        Assert.parametersNotNull("val", suffix);
        return addCriteria(FilterOperator.EQUAL, Pattern.compile(suffix + "$", Pattern.CASE_INSENSITIVE));
    }

    @Override
    public T equal(final Object val) {
        return addCriteria(FilterOperator.EQUAL, val);
    }

    @Override
    public T equalIgnoreCase(final Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(FilterOperator.EQUAL, Pattern.compile("^" + val + "$", Pattern.CASE_INSENSITIVE));
    }

    @Override
    public T exists() {
        return addCriteria(FilterOperator.EXISTS, true);
    }

    @Override
    public T greaterThan(final Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(FilterOperator.GREATER_THAN, val);
    }

    @Override
    public T greaterThanOrEq(final Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(FilterOperator.GREATER_THAN_OR_EQUAL, val);
    }

    @Override
    public T hasAllOf(final Iterable<?> values) {
        Assert.parametersNotNull("values", values);
        Assert.parameterNotEmpty("values", values);
        return addCriteria(FilterOperator.ALL, values);
    }

    @Override
    public T hasAnyOf(final Iterable<?> values) {
        Assert.parametersNotNull("values", values);
        if (LOG.isWarningEnabled()) {
            if (!values.iterator().hasNext()) {
                LOG.warning("Specified an empty list/collection with the '" + field + "' criteria");
            }
        }
        return addCriteria(FilterOperator.IN, values);
    }

    @Override
    public T hasNoneOf(final Iterable<?> values) {
        Assert.parametersNotNull("values", values);
        Assert.parameterNotEmpty("values", values);
        return addCriteria(FilterOperator.NOT_IN, values);
    }

    @Override
    public T hasThisElement(final Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(FilterOperator.ELEMENT_MATCH, val);
    }

    @Override
    public T hasThisOne(final Object val) {
        return addCriteria(FilterOperator.EQUAL, val);
    }

    @Override
    public T in(final Iterable<?> values) {
        return hasAnyOf(values);
    }

    @Override
    public T intersects(final Geometry geometry) {
        target.add(new StandardGeoFieldCriteria(query, field, INTERSECTS, geometry, null, validateName, false));
        return target;
    }

    @Override
    public T intersects(final Geometry geometry, final CoordinateReferenceSystem crs) {
        target.add(new StandardGeoFieldCriteria(query, field, INTERSECTS, geometry, null, validateName, false, crs));
        return target;
    }

    @Override
    public T lessThan(final Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(FilterOperator.LESS_THAN, val);
    }

    @Override
    public T lessThanOrEq(final Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(FilterOperator.LESS_THAN_OR_EQUAL, val);
    }

    @Override
    public T mod(final long divisor, final long remainder) {
        return addCriteria(FilterOperator.MOD, new long[]{divisor, remainder});
    }

    @Override
    public T near(final double longitude, final double latitude) {
        return near(longitude, latitude, false);
    }

    @Override
    public T near(final double longitude, final double latitude, final boolean spherical) {
        return addGeoCriteria(spherical ? FilterOperator.NEAR_SPHERE : FilterOperator.NEAR, new double[]{longitude, latitude}, null);
    }

    @Override
    public T near(final double longitude, final double latitude, final double radius) {
        return near(longitude, latitude, radius, false);
    }

    @Override
    public T near(final double longitude, final double latitude, final double radius, final boolean spherical) {
        return addGeoCriteria(spherical ? FilterOperator.NEAR_SPHERE : FilterOperator.NEAR,
                              new double[]{longitude, latitude},
                              opts("$maxDistance", radius));
    }

    @Override
    public T near(final Point point, final int maxDistance) {
        target.add(new StandardGeoFieldCriteria(query, field, FilterOperator.NEAR, point, maxDistance, validateName, false));
        return target;
    }

    @Override
    public T near(final Point point) {
        target.add(new StandardGeoFieldCriteria(query, field, FilterOperator.NEAR, point, null, validateName, false));
        return target;
    }

    @Override
    public FieldEnd<T> not() {
        not = !not;
        return this;
    }

    @Override
    public T notEqual(final Object val) {
        return addCriteria(FilterOperator.NOT_EQUAL, val);
    }

    @Override
    public T notIn(final Iterable<?> values) {
        return hasNoneOf(values);
    }

    @Override
    public T sizeEq(final int val) {
        return addCriteria(FilterOperator.SIZE, val);
    }

    @Override
    public T startsWith(final String prefix) {
        Assert.parametersNotNull("val", prefix);
        return addCriteria(FilterOperator.EQUAL, Pattern.compile("^" + prefix));
    }

    @Override
    public T startsWithIgnoreCase(final String prefix) {
        Assert.parametersNotNull("val", prefix);
        return addCriteria(FilterOperator.EQUAL, Pattern.compile("^" + prefix, Pattern.CASE_INSENSITIVE));
    }

    @Override
    public T type(final Type type) {
        return addCriteria(FilterOperator.TYPE, type.val());
    }

    @Override
    public T within(final Shape shape) {
        Assert.parametersNotNull("shape", shape);
        return addCriteria(GEO_WITHIN, shape.toDBObject());
    }

    @Override
    public T within(final Polygon boundary) {
        target.add(new StandardGeoFieldCriteria(query, field, GEO_WITHIN, boundary, null, validateName, false));
        return target;
    }

    @Override
    public T within(final MultiPolygon boundaries) {
        target.add(new StandardGeoFieldCriteria(query, field, GEO_WITHIN, boundaries, null, validateName, false));
        return target;
    }

    @Override
    public T within(final Polygon boundary, final CoordinateReferenceSystem crs) {
        target.add(new StandardGeoFieldCriteria(query, field, GEO_WITHIN, boundary, null, validateName, false, crs));
        return target;
    }

    @Override
    public T within(final MultiPolygon boundaries, final CoordinateReferenceSystem crs) {
        target.add(new StandardGeoFieldCriteria(query, field, GEO_WITHIN, boundaries, null, validateName, false, crs));
        return target;
    }

    /**
     * Add a criteria
     */
    private T addCriteria(final FilterOperator op, final Object val) {
        target.add(new FieldCriteria(query, field, op, val, validateName, query.isValidatingTypes(), not));
        return target;
    }

    private T addGeoCriteria(final FilterOperator op, final Object val, final Map<String, Object> opts) {
        if (not) {
            throw new QueryException("Geospatial queries cannot be negated with 'not'.");
        }

        target.add(new GeoFieldCriteria(query, field, op, val, validateName, false, opts));
        return target;
    }

    private Map<String, Object> opts(final String s, final Object v) {
        final Map<String, Object> opts = new HashMap<String, Object>();
        opts.put(s, v);
        return opts;
    }
}

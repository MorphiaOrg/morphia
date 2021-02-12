package dev.morphia.query;


import com.mongodb.client.model.geojson.CoordinateReferenceSystem;
import com.mongodb.client.model.geojson.MultiPolygon;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.lang.Nullable;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.utils.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.quote;

/**
 * Represents a document field in a query and presents the operations available to querying against that field.  This is an internal
 * class and subject to change without notice.
 *
 * @param <T> the type of the CriteriaContainer
 * @morphia.internal
 */
@SuppressWarnings("removal")
@Deprecated(since = "2.0", forRemoval = true)
public class FieldEndImpl<T extends CriteriaContainer> implements FieldEnd<T> {
    private static final Logger LOG = LoggerFactory.getLogger(FieldEndImpl.class);
    private final String field;
    private final T target;
    private final EntityModel model;
    private final boolean validating;
    private final Mapper mapper;
    private boolean not;

    /**
     * Creates a FieldEnd for a particular field.
     *
     * @param field      the field to consider
     * @param target     the CriteriaContainer
     * @param model      the mapped class
     * @param validating validate names or not
     */
    protected FieldEndImpl(Mapper mapper, String field, T target, EntityModel model, boolean validating) {
        this.mapper = mapper;
        this.field = field;
        this.target = target;
        this.model = model;
        this.validating = validating;
    }

    @Override
    public T contains(String string) {
        Assert.parametersNotNull("val", string);
        return addCriteria(FilterOperator.EQUAL, compile(quote(string)));
    }

    @Override
    public T containsIgnoreCase(String string) {
        Assert.parametersNotNull("val", string);
        return addCriteria(FilterOperator.EQUAL, compile(quote(string), CASE_INSENSITIVE));
    }

    @Override
    public T doesNotExist() {
        return addCriteria(FilterOperator.EXISTS, false);
    }

    @Override
    public T endsWith(String suffix) {
        Assert.parametersNotNull("val", suffix);
        return addCriteria(FilterOperator.EQUAL, compile(quote(suffix) + "$"));
    }

    @Override
    public T endsWithIgnoreCase(String suffix) {
        Assert.parametersNotNull("val", suffix);
        return addCriteria(FilterOperator.EQUAL, compile(quote(suffix) + "$", CASE_INSENSITIVE));
    }

    @Override
    public T equal(Object val) {
        return addCriteria(FilterOperator.EQUAL, val);
    }

    @Override
    public T equalIgnoreCase(Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(FilterOperator.EQUAL, compile("^" + quote(val.toString()) + "$", CASE_INSENSITIVE));
    }

    @Override
    public T exists() {
        return addCriteria(FilterOperator.EXISTS, true);
    }

    @Override
    public T greaterThan(Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(FilterOperator.GREATER_THAN, val);
    }

    @Override
    public T greaterThanOrEq(Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(FilterOperator.GREATER_THAN_OR_EQUAL, val);
    }

    @Override
    public T hasAllOf(Iterable<?> values) {
        Assert.parametersNotNull("values", values);
        Assert.parameterNotEmpty("values", values);
        return addCriteria(FilterOperator.ALL, values);
    }

    @Override
    public T hasAnyOf(Iterable<?> values) {
        Assert.parametersNotNull("values", values);
        Assert.parameterNotEmpty("values", values);

        return addCriteria(FilterOperator.IN, values);
    }

    @Override
    public T hasNoneOf(Iterable<?> values) {
        Assert.parametersNotNull("values", values);
        Assert.parameterNotEmpty("values", values);
        return addCriteria(FilterOperator.NOT_IN, values);
    }

    @Override
    public T elemMatch(Query query) {
        Assert.parametersNotNull("query", query);
        return addCriteria(FilterOperator.ELEMENT_MATCH, query, not);
    }

    @Override
    public T hasThisOne(Object val) {
        return addCriteria(FilterOperator.EQUAL, val);
    }

    @Override
    public T in(Iterable<?> values) {
        return hasAnyOf(values);
    }

    @Override
    public T intersects(com.mongodb.client.model.geojson.Geometry geometry) {
        target.add(Geo2dSphereCriteria.geo(mapper, field, FilterOperator.INTERSECTS, geometry, model, validating));
        return target;
    }

    @Override
    public T intersects(com.mongodb.client.model.geojson.Geometry geometry, CoordinateReferenceSystem crs) {
        target.add(Geo2dSphereCriteria.geo(mapper, field, FilterOperator.INTERSECTS, geometry, model, validating)
                                      .addCoordinateReferenceSystem(crs));
        return target;
    }

    @Override
    public T lessThan(Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(FilterOperator.LESS_THAN, val);
    }

    @Override
    public T lessThanOrEq(Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(FilterOperator.LESS_THAN_OR_EQUAL, val);
    }

    @Override
    public T mod(long divisor, long remainder) {
        return addCriteria(FilterOperator.MOD, new long[]{divisor, remainder});
    }

    @Override
    public T near(double longitude, double latitude) {
        return near(longitude, latitude, false);
    }

    @Override
    public T near(double longitude, double latitude, boolean spherical) {
        return addGeoCriteria(spherical
                              ? FilterOperator.NEAR_SPHERE
                              : FilterOperator.NEAR, new double[]{longitude, latitude}, new HashMap<>());
    }

    @Override
    public T near(double longitude, double latitude, double radius) {
        return near(longitude, latitude, radius, false);
    }

    @Override
    public T near(double longitude, double latitude, double radius, boolean spherical) {
        return addGeoCriteria(spherical ? FilterOperator.NEAR_SPHERE : FilterOperator.NEAR, new double[]{longitude, latitude},
            Map.of("$maxDistance", radius));
    }

    @Override
    public T near(com.mongodb.client.model.geojson.Point point) {
        target.add(Geo2dSphereCriteria.geo(mapper, field, FilterOperator.NEAR, point, model, validating));
        return target;
    }

    @Override
    public T near(com.mongodb.client.model.geojson.Point point, Double maxDistance, Double minDistance) {
        target.add(Geo2dSphereCriteria.geo(mapper, field, FilterOperator.NEAR, point, model, validating)
                                      .maxDistance(maxDistance)
                                      .minDistance(minDistance));
        return target;
    }

    @Override
    public T nearSphere(com.mongodb.client.model.geojson.Point point) {
        return nearSphere(point, null, null);
    }

    @Override
    public T nearSphere(com.mongodb.client.model.geojson.Point point, @Nullable Double maxDistance, @Nullable Double minDistance) {
        target.add(Geo2dSphereCriteria.geo(mapper, field, FilterOperator.NEAR_SPHERE, point, model, validating)
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
    public T notEqual(Object val) {
        return addCriteria(FilterOperator.NOT_EQUAL, val);
    }

    @Override
    public T notIn(Iterable<?> values) {
        return hasNoneOf(values);
    }

    @Override
    public T sizeEq(int val) {
        return addCriteria(FilterOperator.SIZE, val);
    }

    @Override
    public T startsWith(String prefix) {
        Assert.parametersNotNull("val", prefix);
        /*LITERAL*/
        return addCriteria(FilterOperator.EQUAL, compile("^" + quote(prefix)));
    }

    @Override
    public T startsWithIgnoreCase(String prefix) {
        Assert.parametersNotNull("val", prefix);
        /*  | LITERAL */
        return addCriteria(FilterOperator.EQUAL, compile("^" + quote(prefix), CASE_INSENSITIVE));
    }

    @Override
    public T type(Type type) {
        return addCriteria(FilterOperator.TYPE, type);
    }

    @Override
    public T within(Shape shape) {
        Assert.parametersNotNull("shape", shape);
        return addCriteria(FilterOperator.GEO_WITHIN, shape);
    }

    @Override
    public T within(Polygon boundary) {
        target.add(Geo2dSphereCriteria.geo(mapper, field, FilterOperator.GEO_WITHIN, boundary, model,
            validating));
        return target;
    }

    @Override
    public T within(MultiPolygon boundaries) {
        target.add(Geo2dSphereCriteria.geo(mapper, field, FilterOperator.GEO_WITHIN, boundaries, model, validating));
        return target;
    }

    @Override
    public T within(Polygon boundary, CoordinateReferenceSystem crs) {
        target.add(Geo2dSphereCriteria.geo(mapper, field, FilterOperator.GEO_WITHIN, boundary, model, validating)
                                      .addCoordinateReferenceSystem(crs));
        return target;
    }

    @Override
    public T within(MultiPolygon boundaries, CoordinateReferenceSystem crs) {
        target.add(Geo2dSphereCriteria.geo(mapper, field, FilterOperator.GEO_WITHIN, boundaries, model, validating)
                                      .addCoordinateReferenceSystem(crs));
        return target;
    }

    protected T addCriteria(FilterOperator op, Object val) {
        return addCriteria(op, val, not);
    }

    protected T addCriteria(FilterOperator op, Object val, boolean not) {
        target.add(new FieldCriteria(mapper, field, op, val, not, model, validating));
        return target;
    }

    protected T addGeoCriteria(FilterOperator op, Object val, Map<String, Object> opts) {
        if (not) {
            throw new QueryException("Geospatial queries cannot be negated with 'not'.");
        }

        target.add(new Geo2dCriteria(mapper, field, op, val, opts, model, validating));
        return target;
    }

    protected String getField() {
        return field;
    }

    protected boolean isNot() {
        return not;
    }
}

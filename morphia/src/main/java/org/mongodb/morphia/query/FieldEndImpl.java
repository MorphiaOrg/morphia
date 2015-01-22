package org.mongodb.morphia.query;


import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.utils.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


public class FieldEndImpl<T extends CriteriaContainerImpl> implements FieldEnd<T> {
  private static final Logger LOG = MorphiaLoggerFactory.get(FieldEndImpl.class);

  private final QueryImpl<?> query;
  private final String       field;
  private final T            target;
  private boolean      not;
  private final boolean      validateName;

  private FieldEndImpl(final QueryImpl<?> query, final String field, final T target, final boolean validateName, final boolean not) {
    this.query = query;
    this.field = field;
    this.target = target;
    this.validateName = validateName;
    this.not = not;
  }

  public FieldEndImpl(final QueryImpl<?> query, final String field, final T target, final boolean validateName) {

    this(query, field, target, validateName, false);
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

  public FieldEnd<T> not() {
    not = !not;
    return this;
  }

  public T startsWith(final String prefix) {
    Assert.parametersNotNull("val", prefix);
    return addCriteria(FilterOperator.EQUAL, Pattern.compile("^" + prefix));
  }

  public T startsWithIgnoreCase(final String prefix) {
    Assert.parametersNotNull("val", prefix);
    return addCriteria(FilterOperator.EQUAL, Pattern.compile("^" + prefix, Pattern.CASE_INSENSITIVE));
  }

  public T endsWith(final String suffix) {
    Assert.parametersNotNull("val", suffix);
    return addCriteria(FilterOperator.EQUAL, Pattern.compile(suffix + "$"));
  }

  public T endsWithIgnoreCase(final String suffix) {
    Assert.parametersNotNull("val", suffix);
    return addCriteria(FilterOperator.EQUAL, Pattern.compile(suffix + "$", Pattern.CASE_INSENSITIVE));
  }

  public T contains(final String string) {
    Assert.parametersNotNull("val", string);
    return addCriteria(FilterOperator.EQUAL, Pattern.compile(string));
  }

  public T containsIgnoreCase(final String string) {
    Assert.parametersNotNull("val", string);
    return addCriteria(FilterOperator.EQUAL, Pattern.compile(string, Pattern.CASE_INSENSITIVE));
  }

  public T exists() {
    return addCriteria(FilterOperator.EXISTS, true);
  }

  public T doesNotExist() {
    return addCriteria(FilterOperator.EXISTS, false);
  }

  public T equal(final Object val) {
    return addCriteria(FilterOperator.EQUAL, val);
  }

  public T within(final Shape shape) {
    Assert.parametersNotNull("shape", shape);
    return addCriteria(FilterOperator.GEO_WITHIN, shape.toDBObject());
  }

  public T greaterThan(final Object val) {
    Assert.parametersNotNull("val", val);
    return addCriteria(FilterOperator.GREATER_THAN, val);
  }

  public T greaterThanOrEq(final Object val) {
    Assert.parametersNotNull("val", val);
    return addCriteria(FilterOperator.GREATER_THAN_OR_EQUAL, val);
  }

  public T hasThisOne(final Object val) {
    return addCriteria(FilterOperator.EQUAL, val);
  }

  public T hasAllOf(final Iterable<?> values) {
    Assert.parametersNotNull("values", values);
    Assert.parameterNotEmpty(values, "values");
    return addCriteria(FilterOperator.ALL, values);
  }

  public T hasAnyOf(final Iterable<?> values) {
    Assert.parametersNotNull("values", values);
    if (LOG.isWarningEnabled()) {
      if (!values.iterator().hasNext()) {
        LOG.warning("Specified an empty list/collection with the '" + field + "' criteria");
      }
    }
    return addCriteria(FilterOperator.IN, values);
  }

  public T in(final Iterable<?> values) {
    return hasAnyOf(values);
  }

  public T mod(final long divisor, final long remainder) {
    return addCriteria(FilterOperator.MOD, new long[] {divisor, remainder});
  }

  public T hasThisElement(final Object val) {
    Assert.parametersNotNull("val", val);
    return addCriteria(FilterOperator.ELEMENT_MATCH, val);
  }

  public T hasNoneOf(final Iterable<?> values) {
    Assert.parametersNotNull("values", values);
    Assert.parameterNotEmpty(values, "values");
    return addCriteria(FilterOperator.NOT_IN, values);
  }

  public T notIn(final Iterable<?> values) {
    return hasNoneOf(values);
  }

  public T lessThan(final Object val) {
    Assert.parametersNotNull("val", val);
    return addCriteria(FilterOperator.LESS_THAN, val);
  }

  public T lessThanOrEq(final Object val) {
    Assert.parametersNotNull("val", val);
    return addCriteria(FilterOperator.LESS_THAN_OR_EQUAL, val);
  }

  public T notEqual(final Object val) {
    return addCriteria(FilterOperator.NOT_EQUAL, val);
  }

  public T sizeEq(final int val) {
    return addCriteria(FilterOperator.SIZE, val);
  }

  public T near(final double x, final double y) {
    return near(x, y, false);
  }

  public T near(final double x, final double y, final double radius) {
    return near(x, y, radius, false);
  }

  public T near(final double x, final double y, final double radius, final boolean spherical) {
    return addGeoCriteria(spherical ? FilterOperator.NEAR_SPHERE : FilterOperator.NEAR, new double[] {x, y}, opts("$maxDistance", radius));
  }

  public T near(final double x, final double y, final boolean spherical) {
    return addGeoCriteria(spherical ? FilterOperator.NEAR_SPHERE : FilterOperator.NEAR, new double[] {x, y}, null);
  }

  private Map<String, Object> opts(final String s, final Object v) {
    final Map<String, Object> opts = new HashMap<String, Object>();
    opts.put(s, v);
    return opts;
  }
}
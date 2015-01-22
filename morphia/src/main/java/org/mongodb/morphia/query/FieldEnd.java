package org.mongodb.morphia.query;


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

  T startsWithIgnoreCase(String prefix);

  /**
   * This implements the $geoWithin operator and is only compatible with mongo 2.4 or greater.
   */
  T within(Shape shape);
  
  T type(Type type);
}
package com.google.code.morphia.query;


public interface FieldPart<T> {
	
	Query<T> exists();
	Query<T> doesNotExist();
	Query<T> greaterThan(Object val);	
	Query<T> greaterThanOrEq(Object val);	
	Query<T> lessThan(Object val);
	Query<T> lessThanOrEq(Object val);
	Query<T> equal(Object val);
	Query<T> notEqual(Object val);

	Query<T> hasThisOne(Object val);
	Query<T> hasAllOf(Iterable<?> vals);
	Query<T> hasAnyOf(Iterable<?> vals);
	Query<T> hasNoneOf(Iterable<?> vals);
}

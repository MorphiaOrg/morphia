package com.google.code.morphia.query;

public interface FieldEnd<T> {
	
	T exists();
	T doesNotExist();
	T greaterThan(Object val);	
	T greaterThan(Object val, boolean validateType);	
	T greaterThanOrEq(Object val);	
	T greaterThanOrEq(Object val, boolean validateType);	
	T lessThan(Object val);
	T lessThan(Object val, boolean validateType);
	T lessThanOrEq(Object val);
	T lessThanOrEq(Object val, boolean validateType);
	T equal(Object val);
	T equal(Object val, boolean validateType);
	T notEqual(Object val);
	T notEqual(Object val, boolean validateType);

	T startsWith(String prefix);
	T startsWith(String prefix, boolean validateType);
	T startsWithIgnoreCase(String prefix);
	T startsWithIgnoreCase(String prefix, boolean validateType);
	T endsWith(String suffix);
	T endsWith(String suffix, boolean validateType);
	T endsWithIgnoreCase(String suffix);
	T endsWithIgnoreCase(String suffix, boolean validateType);
	T contains(String string);
	T contains(String string, boolean validateType);
	T containsIgnoreCase(String suffix);
	T containsIgnoreCase(String suffix, boolean validateType);
	
	T hasThisOne(Object val);
	T hasThisOne(Object val, boolean validateType);
	T hasAllOf(Iterable<?> vals);
	T hasAllOf(Iterable<?> vals, boolean validateType);
	T hasAnyOf(Iterable<?> vals);
	T hasAnyOf(Iterable<?> vals, boolean validateType);
	T hasNoneOf(Iterable<?> vals);
	T hasNoneOf(Iterable<?> vals, boolean validateType);

	T in(Iterable<?> vals);
	T in(Iterable<?> vals, boolean validateType);

	T notIn(Iterable<?> vals);
	T notIn(Iterable<?> vals, boolean validateType);

	T hasThisElement(Object val);
	T hasThisElement(Object val, boolean validateType);
	T sizeEq(int val);
	T sizeEq(int val, boolean validateType);
}

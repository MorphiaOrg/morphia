package com.google.code.morphia.query;

import java.util.regex.Pattern;

import com.google.code.morphia.utils.Assert;

public class FieldEndImpl<T extends CriteriaContainerImpl> implements FieldEnd<T> {

	private QueryImpl<?> query;
	private String field;
	private T target;
	
	private boolean validateName;
	
	public FieldEndImpl(QueryImpl<?> query, String field, T target, boolean validateName) {
		this.query = query;
		this.field = field;
		this.target = target;
		this.validateName = validateName;
	}

	/** Add a criteria */
	private T addCrit(FilterOperator op, Object val) {
		target.add(new FieldCriteria(query, field, op, val, validateName, query.isValidatingTypes()));
		return target;		
	}

	public T startsWith(String prefix) {
		Assert.parametersNotNull("val", prefix);
		return addCrit(FilterOperator.EQUAL, Pattern.compile("^" + prefix));
	}

	public T startsWithIgnoreCase(String prefix) {
		Assert.parametersNotNull("val", prefix);
		return addCrit(FilterOperator.EQUAL, Pattern.compile("^" + prefix, Pattern.CASE_INSENSITIVE));
	}

	public T endsWith(String suffix) {
		Assert.parametersNotNull("val", suffix);
		return addCrit(FilterOperator.EQUAL, Pattern.compile(suffix + "$"));
	}

	public T endsWithIgnoreCase(String suffix) {
		Assert.parametersNotNull("val", suffix);
		return addCrit(FilterOperator.EQUAL, Pattern.compile(suffix + "$", Pattern.CASE_INSENSITIVE));
	}
	
	public T contains(String string) {
		Assert.parametersNotNull("val", string);
		return addCrit(FilterOperator.EQUAL, Pattern.compile(string));
	}

	public T containsIgnoreCase(String string) {
		Assert.parametersNotNull("val", string);
		return addCrit(FilterOperator.EQUAL, Pattern.compile(string, Pattern.CASE_INSENSITIVE));
	}

	public T exists() {
		return addCrit(FilterOperator.EXISTS, true);
	}

	public T doesNotExist() {
		return addCrit(FilterOperator.EXISTS, false);
	}

	public T equal(Object val) {
		return addCrit(FilterOperator.EQUAL, val);
	}
	
	public T greaterThan(Object val) {
		Assert.parametersNotNull("val",val);
		return addCrit(FilterOperator.GREATER_THAN, val);
	}

	public T greaterThanOrEq(Object val) {
		Assert.parametersNotNull("val",val);
		return addCrit(FilterOperator.GREATER_THAN_OR_EQUAL, val);
	}

	public T hasThisOne(Object val) {
		return addCrit(FilterOperator.EQUAL, val);
	}

	public T hasAllOf(Iterable<?> vals) {
		Assert.parametersNotNull("vals",vals);
		Assert.parameterNotEmpty(vals,"vals");
		return addCrit(FilterOperator.ALL, vals);
	}

	public T hasAnyOf(Iterable<?> vals) {
		Assert.parametersNotNull("vals",vals);
		Assert.parameterNotEmpty(vals,"vals");
		return addCrit(FilterOperator.IN, vals);
	}
	
	public T in(Iterable<?> vals) {
		return this.hasAnyOf(vals);
	}

	public T hasThisElement(Object val) {
		Assert.parametersNotNull("val",val);
		return addCrit(FilterOperator.ELEMENT_MATCH, val);
	}

	public T hasNoneOf(Iterable<?> vals) {
		Assert.parametersNotNull("vals",vals);
		Assert.parameterNotEmpty(vals,"vals");
		return addCrit(FilterOperator.NOT_IN, vals);
	}
	
	public T notIn(Iterable<?> vals) {
		return this.hasNoneOf(vals);
	}

	public T lessThan(Object val) {
		Assert.parametersNotNull("val",val);
		return addCrit(FilterOperator.LESS_THAN, val);
	}

	public T lessThanOrEq(Object val) {
		Assert.parametersNotNull("val",val);
		return addCrit(FilterOperator.LESS_THAN_OR_EQUAL, val);
	}

	public T notEqual(Object val) {
		return addCrit(FilterOperator.NOT_EQUAL, val);
	}

	public T sizeEq(int val) {
		Assert.parametersNotNull("val",val);
		return addCrit(FilterOperator.SIZE, val);
	}
	
	public T near(double x, double y) {
		return addCrit(FilterOperator.NEAR, new double[] {x,y});
	}
	
	public T near(double x, double y, boolean spherical) {
		throw new RuntimeException();
	}	
	public T near(double x, double y, double radius) {
		throw new RuntimeException();
	}
	public T near(double x, double y, double radius, boolean spherical) {
		throw new RuntimeException();
	}
	public T near(double x1, double y1, double x2, double y2) {
		throw new RuntimeException();
	}
}

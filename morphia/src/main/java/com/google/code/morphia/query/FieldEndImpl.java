package com.google.code.morphia.query;

import java.util.ArrayList;
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
	
	private T startsWith(String prefix, boolean validate) {
		Assert.parametersNotNull("prefix",prefix);
		this.target.add(new FieldCriteria(this.query, this.field, FilterOperator.EQUAL, Pattern.compile("^" + prefix), this.validateName, validate));
		return this.target;
	}

	public T startsWith(String prefix) {
		return this.startsWith(prefix, this.query.isValidatingTypes());
	}

	private T startsWithIgnoreCase(String prefix, boolean validate) {
		Assert.parametersNotNull("prefix",prefix);
		this.target.add(new FieldCriteria(this.query, this.field, FilterOperator.EQUAL, Pattern.compile("^" + prefix, Pattern.CASE_INSENSITIVE), this.validateName, validate));
		return this.target;
	}

	public T startsWithIgnoreCase(String prefix) {
		return this.startsWithIgnoreCase(prefix, this.query.isValidatingTypes());
	}
	
	private T endsWith(String suffix, boolean validate) {
		Assert.parametersNotNull("suffix", suffix);
		this.target.add(new FieldCriteria(this.query, this.field, FilterOperator.EQUAL, Pattern.compile(suffix + "$"), this.validateName, validate));
		return this.target;
	}

	public T endsWith(String suffix) {
		return this.endsWith(suffix, this.query.isValidatingTypes());
	}

	private T endsWithIgnoreCase(String suffix, boolean validate) {
		Assert.parametersNotNull("suffix", suffix);
		this.target.add(new FieldCriteria(this.query, this.field, FilterOperator.EQUAL, Pattern.compile(suffix + "$", Pattern.CASE_INSENSITIVE), this.validateName, validate));
		return this.target;
	}
	
	public T endsWithIgnoreCase(String suffix) {
		return this.endsWithIgnoreCase(suffix, this.query.isValidatingTypes());
	}

	private T contains(String string, boolean validate) {
		Assert.parametersNotNull("string", string);
		this.target.add(new FieldCriteria(this.query, this.field, FilterOperator.EQUAL, Pattern.compile(string), this.validateName, validate));
		return this.target;
	}
	
	public T contains(String string) {
		return this.contains(string, this.query.isValidatingTypes());
	}

	private T containsIgnoreCase(String string, boolean validate) {
		Assert.parametersNotNull("string", string);
		this.target.add(new FieldCriteria(this.query, this.field, FilterOperator.EQUAL, Pattern.compile(string, Pattern.CASE_INSENSITIVE), this.validateName, validate));
		return this.target;
	}

	public T containsIgnoreCase(String string) {
		return this.containsIgnoreCase(string, this.query.isValidatingTypes());
	}

	public T exists() {
		this.target.add(new FieldCriteria(this.query, this.field, FilterOperator.EXISTS, true, this.validateName, this.query.isValidatingTypes()));
		return this.target;
	}

	public T doesNotExist() {
		this.target.add(new FieldCriteria(this.query, this.field, FilterOperator.EXISTS, false, this.validateName, this.query.isValidatingTypes()));
		return this.target;
	}

	private T equal(Object val, boolean validate) {
		this.target.add(new FieldCriteria(this.query, this.field, FilterOperator.EQUAL, val, this.validateName, validate));
		return this.target;
	}

	public T equal(Object val) {
		return this.equal(val, this.query.isValidatingTypes());
	}
	
	private T near(int x, int y, boolean validate) {
		ArrayList<Integer> point = new ArrayList<Integer>(2);
		point.add(x); point.add(y);
		this.target.add(new FieldCriteria(this.query, this.field, FilterOperator.NEAR, point, this.validateName, validate));
		return this.target;
	}
	
	public T near (int x, int y) {
		return this.near(x, y, this.query.isValidatingTypes());
	}

	private T greaterThan(Object val, boolean validate) {
		Assert.parametersNotNull("val",val);
		this.target.add(new FieldCriteria(this.query, this.field, FilterOperator.GREATER_THAN, val, this.validateName, validate));
		return this.target;
	}

	public T greaterThan(Object val) {
		return this.greaterThan(val, this.query.isValidatingTypes());
	}

	private T greaterThanOrEq(Object val, boolean validate) {
		Assert.parametersNotNull("val",val);
		this.target.add(new FieldCriteria(this.query, this.field, FilterOperator.GREATER_THAN_OR_EQUAL, val, this.validateName, validate));
		return this.target;
	}

	public T greaterThanOrEq(Object val) {
		return this.greaterThanOrEq(val, this.query.isValidatingTypes());
	}

	private T hasThisOne(Object val, boolean validate) {
		this.target.add(new FieldCriteria(this.query, this.field, FilterOperator.EQUAL, val, this.validateName, validate));
		return this.target;
	}

	public T hasThisOne(Object val) {
		return this.hasThisOne(val, this.query.isValidatingTypes());
	}

	private T hasAllOf(Iterable<?> vals, boolean validate) {
		Assert.parametersNotNull("vals",vals);
		Assert.parameterNotEmpty(vals,"vals");
		this.target.add(new FieldCriteria(this.query, this.field, FilterOperator.ALL, vals, this.validateName, validate));
		return this.target;
	}

	public T hasAllOf(Iterable<?> vals) {
		return this.hasAllOf(vals, this.query.isValidatingTypes());
	}

	private T hasAnyOf(Iterable<?> vals, boolean validate) {
		Assert.parametersNotNull("vals",vals);
		Assert.parameterNotEmpty(vals,"vals");
		this.target.add(new FieldCriteria(this.query, this.field, FilterOperator.IN, vals, this.validateName, validate));
		return this.target;
	}

	public T hasAnyOf(Iterable<?> vals) {
		return this.hasAnyOf(vals, this.query.isValidatingTypes());
	}
	
	public T in(Iterable<?> vals) {
		return this.hasAnyOf(vals, this.query.isValidatingTypes());
	}

	private T hasThisElement(Object val, boolean validate) {
		Assert.parametersNotNull("val",val);
		this.target.add(new FieldCriteria(this.query, this.field, FilterOperator.ELEMENT_MATCH, val, this.validateName, validate));
		return this.target;
	}

	public T hasThisElement(Object val) {
		return this.hasThisElement(val, this.query.isValidatingTypes());
	}

	private T hasNoneOf(Iterable<?> vals, boolean validate) {
		Assert.parametersNotNull("vals",vals);
		Assert.parameterNotEmpty(vals,"vals");
		this.target.add(new FieldCriteria(this.query, this.field, FilterOperator.NOT_IN, vals, this.validateName, validate));
		return this.target;
	}

	public T hasNoneOf(Iterable<?> vals) {
		return this.hasNoneOf(vals, this.query.isValidatingTypes());
	}
	
	public T notIn(Iterable<?> vals) {
		return this.hasNoneOf(vals, this.query.isValidatingTypes());
	}

	private T lessThan(Object val, boolean validate) {
		Assert.parametersNotNull("val",val);
		this.target.add(new FieldCriteria(this.query, this.field, FilterOperator.LESS_THAN, val, this.validateName, validate));
		return this.target;
	}

	public T lessThan(Object val) {
		return this.lessThan(val, this.query.isValidatingTypes());
	}

	private T lessThanOrEq(Object val, boolean validate) {
		Assert.parametersNotNull("val",val);
		this.target.add(new FieldCriteria(this.query, this.field, FilterOperator.LESS_THAN_OR_EQUAL, val, this.validateName, validate));
		return this.target;
	}

	public T lessThanOrEq(Object val) {
		return this.lessThanOrEq(val, this.query.isValidatingTypes());
	}

	private T notEqual(Object val, boolean validate) {
		this.target.add(new FieldCriteria(this.query, this.field, FilterOperator.NOT_EQUAL, val, this.validateName, validate));
		return this.target;
	}

	public T notEqual(Object val) {
		return this.notEqual(val, this.query.isValidatingTypes());
	}

	private T sizeEq(int val, boolean validate) {
		Assert.parametersNotNull("val",val);
		this.target.add(new FieldCriteria(this.query, this.field, FilterOperator.SIZE, val, this.validateName, validate));
		return this.target;
	}

	public T sizeEq(int val) {
		return this.sizeEq(val, this.query.isValidatingTypes());
	}
}

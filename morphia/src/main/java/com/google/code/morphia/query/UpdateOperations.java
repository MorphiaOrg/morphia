package com.google.code.morphia.query;

import java.util.List;

public interface UpdateOperations<T> {
	UpdateOperations<T> set(String fieldExpr, Object value);
	UpdateOperations<T> unset(String fieldExpr);

	UpdateOperations<T> add(String fieldExpr, Object value);
	UpdateOperations<T> add(String fieldExpr, Object value, boolean addDups);
	UpdateOperations<T> addAll(String fieldExpr, List<?> values, boolean addDups);
	
	UpdateOperations<T> removeFirst(String fieldExpr);
	UpdateOperations<T> removeLast(String fieldExpr);
	UpdateOperations<T> removeAll(String fieldExpr, Object value);
	UpdateOperations<T> removeAll(String fieldExpr, List<?> values);

	UpdateOperations<T> dec(String fieldExpr);
	UpdateOperations<T> inc(String fieldExpr);
	UpdateOperations<T> inc(String fieldExpr, Number value);
}

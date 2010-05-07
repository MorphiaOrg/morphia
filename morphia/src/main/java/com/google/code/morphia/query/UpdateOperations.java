package com.google.code.morphia.query;

import java.util.List;

public interface UpdateOperations {
	UpdateOperations set(String fieldExpr, Object value);
	UpdateOperations unset(String fieldExpr);

	UpdateOperations add(String fieldExpr, Object value);
	UpdateOperations add(String fieldExpr, Object value, boolean addDups);
	UpdateOperations add(String fieldExpr, List<?> values, boolean addDups);
	
	UpdateOperations removeFirst(String fieldExpr);
	UpdateOperations removeLast(String fieldExpr);
	UpdateOperations removeAll(String fieldExpr, Object value);
	UpdateOperations removeAll(String fieldExpr, List<?> values);

	UpdateOperations dec(String fieldExpr);
	UpdateOperations inc(String fieldExpr);
	UpdateOperations inc(String fieldExpr, Number value);
}

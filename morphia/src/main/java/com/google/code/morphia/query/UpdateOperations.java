package com.google.code.morphia.query;

import java.util.List;

/** <p> 
 * 		A nicer interface to the update operations in monogodb. 
 *      All these operations happen at the server and can cause the server 
 *      and client version of the Entity to be different
 *  </p> 
 **/
public interface UpdateOperations<T> {
	/** sets a the field value */
	UpdateOperations<T> set(String fieldExpr, Object value);
	/** removes the field */
	UpdateOperations<T> unset(String fieldExpr);

	/** adds the value to an array field (on the server)*/
	UpdateOperations<T> add(String fieldExpr, Object value);
	UpdateOperations<T> add(String fieldExpr, Object value, boolean addDups);
	/** adds the values to an array field (on the server) */
	UpdateOperations<T> addAll(String fieldExpr, List<?> values, boolean addDups);
	
	/** removed the first value from the array (on the server)*/
	UpdateOperations<T> removeFirst(String fieldExpr);
	/** removed the last value from the array (on the server)*/
	UpdateOperations<T> removeLast(String fieldExpr);
	/** removed the value from the array field (on the server)*/
	UpdateOperations<T> removeAll(String fieldExpr, Object value);
	/** removed the values from the array field (on the server) */
	UpdateOperations<T> removeAll(String fieldExpr, List<?> values);

	/** decrements the value from the numeric field by 1 (on the server) */
	UpdateOperations<T> dec(String fieldExpr);
	/** increments the value from the numeric field by 1 (on the server) */
	UpdateOperations<T> inc(String fieldExpr);
	/** increments the value from the numeric field by value (negatives are allowed) (on the server) */
	UpdateOperations<T> inc(String fieldExpr, Number value);
}

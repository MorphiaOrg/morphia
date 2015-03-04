package org.mongodb.morphia.query;


import java.util.List;


/**
 * <p> A nicer interface to the update operations in monogodb. All these operations happen at the server and can cause the server and client
 * version of the Entity to be different </p>
 * 
 * @param <T> The Java type used in the updates
 */
public interface UpdateOperations<T> {
  /**
   * sets the field value
   */
  UpdateOperations<T> set(String fieldExpr, Object value);

  /**
   * sets the field on insert.
   */
  UpdateOperations<T> setOnInsert(String fieldExpr, Object value);

  /**
   * removes the field
   */
  UpdateOperations<T> unset(String fieldExpr);

  /**
   * adds the value to an array field
   */
  UpdateOperations<T> add(String fieldExpr, Object value);

  UpdateOperations<T> add(String fieldExpr, Object value, boolean addDups);

  /**
   * adds the values to an array field
   */
  UpdateOperations<T> addAll(String fieldExpr, List<?> values, boolean addDups);

  /**
   * removes the first value from the array
   */
  UpdateOperations<T> removeFirst(String fieldExpr);

  /**
   * removes the last value from the array
   */
  UpdateOperations<T> removeLast(String fieldExpr);

  /**
   * removes the value from the array field
   */
  UpdateOperations<T> removeAll(String fieldExpr, Object value);

  /**
   * removes the values from the array field
   */
  UpdateOperations<T> removeAll(String fieldExpr, List<?> values);

  /**
   * decrements the numeric field by 1
   */
  UpdateOperations<T> dec(String fieldExpr);

  /**
   * increments the numeric field by 1
   */
  UpdateOperations<T> inc(String fieldExpr);

  /**
   * increments the numeric field by value (negatives are allowed)
   */
  UpdateOperations<T> inc(String fieldExpr, Number value);

  /**
   * sets the numeric field to value if it is greater than the current value.
   */
  UpdateOperations<T> max(String fieldExpr, Number value);

  /**
   * sets the numeric field to value if it is less than the current value.
   */
  UpdateOperations<T> min(String fieldExpr, Number value);

  /**
   * Turns on validation (for all calls made after); by default validation is on
   */
  UpdateOperations<T> enableValidation();

  /**
   * Turns off validation (for all calls made after)
   */
  UpdateOperations<T> disableValidation();

  /**
   * Enables isolation (so this update happens in one shot, without yielding)
   */
  UpdateOperations<T> isolated();
}

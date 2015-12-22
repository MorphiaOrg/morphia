package org.mongodb.morphia.query;


import java.util.List;


/**
 * <p> A nicer interface to the update operations in monogodb. All these operations happen at the server and can cause the server and
 * client
 * version of the Entity to be different </p>
 *
 * @param <T> The Java type used in the updates
 */
public interface UpdateOperations<T> {
    /**
     * adds the value to an array field
     *
     * @param fieldExpr the field to update
     * @param value     the value to add
     * @return this
     * @mongodb.driver.manual reference/operator/update/addToSet/ $addToSet
     */
    UpdateOperations<T> add(String fieldExpr, Object value);

    /**
     * adds the value to an array field
     *
     * @param fieldExpr the field to update
     * @param value     the value to add
     * @param addDups   if true, the value will be added even if it already exists in the array ($push)
     * @return this
     * @mongodb.driver.manual reference/operator/update/addToSet/ $addToSet
     * @mongodb.driver.manual reference/operator/update/push/ $push
     */
    UpdateOperations<T> add(String fieldExpr, Object value, boolean addDups);

    /**
     * adds the values to an array field
     *
     * @param fieldExpr the field to update
     * @param values    the values to add
     * @param addDups   if true, the values will be added even if they already exists in the array ($push)
     * @return this
     * @mongodb.driver.manual reference/operator/update/addToSet/ $addToSet
     * @mongodb.driver.manual reference/operator/update/push/ $push
     */
    UpdateOperations<T> addAll(String fieldExpr, List<?> values, boolean addDups);

    /**
     * Decrements the numeric field by 1
     *
     * @param field the field to update
     * @return this
     * @mongodb.driver.manual reference/operator/update/dec/ $dec
     */
    UpdateOperations<T> dec(String field);

    /**
     * Turns off validation (for all calls made after)
     *
     * @return this
     */
    UpdateOperations<T> disableValidation();

    /**
     * Turns on validation (for all calls made after); by default validation is on
     *
     * @return this
     */
    UpdateOperations<T> enableValidation();

    /**
     * Increments the numeric field by 1
     *
     * @param field the field to update
     * @return this
     * @mongodb.driver.manual reference/operator/update/inc/ $inc
     */
    UpdateOperations<T> inc(String field);

    /**
     * increments the numeric field by value (negatives are allowed)
     *
     * @param field the field to update
     * @param value the value to increment by
     * @return this
     * @mongodb.driver.manual reference/operator/update/inc/ $inc
     */
    UpdateOperations<T> inc(String field, Number value);

    /**
     * Enables isolation (so this update happens in one shot, without yielding)
     *
     * @return this
     * @mongodb.driver.manual reference/operator/update/isolated/ $isolated
     */
    UpdateOperations<T> isolated();

    /**
     * Sets the numeric field to value if it is greater than the current value.
     *
     * @param field the field to update
     * @param value the value to use
     * @return this
     * @mongodb.driver.manual reference/operator/update/max/ $max
     */
    UpdateOperations<T> max(String field, Number value);

    /**
     * sets the numeric field to value if it is less than the current value.
     *
     * @param field the field to update
     * @param value the value to use
     * @return this
     * @mongodb.driver.manual reference/operator/update/min/ $min
     */
    UpdateOperations<T> min(String field, Number value);

    /**
     * removes the value from the array field
     *
     * @param field the field to update
     * @param value the value to use
     * @return this
     * @mongodb.driver.manual reference/operator/update/pull/ $pull
     */
    UpdateOperations<T> removeAll(String field, Object value);

    /**
     * removes the values from the array field
     *
     * @param field  the field to update
     * @param values the values to use
     * @return this
     * @mongodb.driver.manual reference/operator/update/pullAll/ $pullAll
     */
    UpdateOperations<T> removeAll(String field, List<?> values);

    /**
     * removes the first value from the array
     *
     * @param field the field to update
     * @return this
     * @mongodb.driver.manual reference/operator/update/pop/ $pop
     */
    UpdateOperations<T> removeFirst(String field);

    /**
     * removes the last value from the array
     *
     * @param field the field to update
     * @return this
     * @mongodb.driver.manual reference/operator/update/pop/ $pop
     */
    UpdateOperations<T> removeLast(String field);

    /**
     * sets the field value
     *
     * @param field the field to update
     * @param value the value to use
     * @return this
     * @mongodb.driver.manual reference/operator/update/set/ $set
     */
    UpdateOperations<T> set(String field, Object value);

    /**
     * sets the field on insert.
     *
     * @param field the field to update
     * @param value the value to use
     * @return this
     * @mongodb.driver.manual reference/operator/update/setOnInsert/ $setOnInsert
     */
    UpdateOperations<T> setOnInsert(String field, Object value);

    /**
     * removes the field
     *
     * @param field the field to update
     * @return this
     * @mongodb.driver.manual reference/operator/update/unset/ $unset
     */
    UpdateOperations<T> unset(String field);
}

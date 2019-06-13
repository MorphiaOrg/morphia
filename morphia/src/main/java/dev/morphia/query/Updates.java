package dev.morphia.query;

import java.util.List;

public interface Updates<Updater extends Updates> {
    /**
     * adds the value to an array field if it doesn't already exist in the array
     *
     * @param field the field to update
     * @param value the value to add
     * @return this
     * @mongodb.driver.manual reference/operator/update/addToSet/ $addToSet
     */
    Updater addToSet(String field, Object value);

    /**
     * adds the values to an array field if they doesn't already exist in the array
     *
     * @param field  the field to update
     * @param values the values to add
     * @return this
     * @mongodb.driver.manual reference/operator/update/addToSet/ $addToSet
     */
    Updater addToSet(String field, List<?> values);

    /**
     * adds the values to an array field if they doesn't already exist in the array
     *
     * @param field  the field to update
     * @param values the values to add
     * @return this
     * @mongodb.driver.manual reference/operator/update/addToSet/ $addToSet
     */
    Updater addToSet(String field, Iterable<?> values);

    /**
     * Decrements the numeric field by 1
     *
     * @param field the field to update
     * @return this
     * @mongodb.driver.manual reference/operator/update/inc/ $inc
     */
    Updater dec(String field);

    /**
     * Decrements the numeric field by value (must be a positive Double,
     * Float, Long, or Integer).
     *
     * @param field the field to update
     * @param value the value to decrement by
     * @throws IllegalArgumentException of the value is not an instance of
     *         Double, Float,Long, or Integer
     * @return this
     * @mongodb.driver.manual reference/operator/update/inc/ $inc
     */
    Updater dec(String field, Number value);

    /**
     * Turns off validation (for all calls made after)
     *
     * @return this
     */
    Updater disableValidation();

    /**
     * Turns on validation (for all calls made after); by default validation is on
     *
     * @return this
     */
    Updater enableValidation();

    /**
     * Increments the numeric field by 1
     *
     * @param field the field to update
     * @return this
     * @mongodb.driver.manual reference/operator/update/inc/ $inc
     */
    Updater inc(String field);

    /**
     * increments the numeric field by value (negatives are allowed)
     *
     * @param field the field to update
     * @param value the value to increment by
     * @return this
     * @mongodb.driver.manual reference/operator/update/inc/ $inc
     */
    Updater inc(String field, Number value);

    /**
     * Sets the numeric field to value if it is greater than the current value.
     *
     * @param field the field to update
     * @param value the value to use
     * @return this
     * @mongodb.driver.manual reference/operator/update/max/ $max
     */
    Updater max(String field, Number value);

    /**
     * sets the numeric field to value if it is less than the current value.
     *
     * @param field the field to update
     * @param value the value to use
     * @return this
     * @mongodb.driver.manual reference/operator/update/min/ $min
     */
    Updater min(String field, Number value);

    /**
     * Adds new values to an array field.
     *
     * @param field the field to updated
     * @param value the value to add
     * @return this
     * @mongodb.driver.manual reference/operator/update/push/ $push
     */
    Updater push(String field, Object value);

    /**
     * Adds new values to an array field at the given position
     *
     * @param field   the field to updated
     * @param value   the value to add
     * @param options the options to apply to the push
     * @return this
     * @mongodb.driver.manual reference/operator/update/push/ $push
     */
    Updater push(String field, Object value, final PushOptions options);

    /**
     * Adds new values to an array field.
     *
     * @param field  the field to updated
     * @param values the values to add
     * @return this
     * @mongodb.driver.manual reference/operator/update/push/ $push
     */
    Updater push(String field, List<?> values);

    /**
     * Adds new values to an array field at the given position
     *
     * @param field   the field to updated
     * @param values  the values to add
     * @param options the options to apply to the push
     * @return this
     * @mongodb.driver.manual reference/operator/update/push/ $push
     */
    Updater push(String field, List<?> values, PushOptions options);

    /**
     * removes the value from the array field
     *
     * @param field the field to update
     * @param value the value to use
     * @return this
     * @mongodb.driver.manual reference/operator/update/pull/ $pull
     */
    Updater removeAll(String field, Object value);

    /**
     * removes the values from the array field
     *
     * @param field  the field to update
     * @param values the values to use
     * @return this
     * @mongodb.driver.manual reference/operator/update/pullAll/ $pullAll
     */
    Updater removeAll(String field, List<?> values);

    /**
     * removes the first value from the array
     *
     * @param field the field to update
     * @return this
     * @mongodb.driver.manual reference/operator/update/pop/ $pop
     */
    Updater removeFirst(String field);

    /**
     * removes the last value from the array
     *
     * @param field the field to update
     * @return this
     * @mongodb.driver.manual reference/operator/update/pop/ $pop
     */
    Updater removeLast(String field);

    /**
     * sets the field value
     *
     * @param field the field to update
     * @param value the value to use
     * @return this
     * @mongodb.driver.manual reference/operator/update/set/ $set
     */
    Updater set(String field, Object value);

    /**
     * sets the field on insert.
     *
     * @param field the field to update
     * @param value the value to use
     * @return this
     * @mongodb.driver.manual reference/operator/update/setOnInsert/ $setOnInsert
     */
    Updater setOnInsert(String field, Object value);

    /**
     * removes the field
     *
     * @param field the field to update
     * @return this
     * @mongodb.driver.manual reference/operator/update/unset/ $unset
     */
    Updater unset(String field);
}

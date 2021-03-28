package dev.morphia.query.experimental.updates;

import dev.morphia.query.Query;
import dev.morphia.query.experimental.filters.Filter;
import dev.morphia.sofia.Sofia;

import java.time.temporal.Temporal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

/**
 * Defines helper methods for specifying operations for updates or findAndModify
 *
 * @see Query#update(UpdateOperator, UpdateOperator...)
 * @see Query#modify(UpdateOperator, UpdateOperator...)
 * @since 2.0
 */
public final class UpdateOperators {
    private UpdateOperators() {
    }

    /**
     * The $addToSet operator adds a value to an array unless the value is already present, in which case $addToSet does nothing to that
     * array.
     *
     * @param field the field to updated
     * @param value the value to add
     * @return the update operator
     * @update.operator $addToSet
     */
    public static AddToSetOperator addToSet(String field, Object value) {
        return new AddToSetOperator(field, value);
    }

    /**
     * The $addToSet operator adds a value to an array unless the value is already present, in which case $addToSet does nothing to that
     * array.  By default, this operator will use $each to add each element in the given list to the stored array in the document.
     *
     * @param field  the field to updated
     * @param values the values to add
     * @return the update operator
     * @update.operator $addToSet
     * @update.operator $each
     */
    public static AddToSetOperator addToSet(String field, List<?> values) {
        return new AddToSetOperator(field, values);
    }

    /**
     * The $bit operator performs a bitwise update of a field. The operator supports bitwise and, bitwise or, and bitwise xor (i.e.
     * exclusive or) operations.
     *
     * @param field the field to update
     * @param value the value to use for the update
     * @return the update operator
     * @update.operator $bit
     * @see #or(String, int)
     * @see #xor(String, int)
     */
    public static UpdateOperator and(String field, int value) {
        return new BitOperator("and", field, value);
    }

    /**
     * The $currentDate operator sets the value of a field to the current date, either as a Date or a timestamp. The default type is Date.
     *
     * @param field the field to set
     * @return the update operator
     * @update.operator $currentDate
     */
    public static CurrentDateOperator currentDate(String field) {
        return new CurrentDateOperator(field);
    }

    /**
     * Decrements the value of the field by one.
     *
     * @param field the field to decrement
     * @return the update operator
     * @update.operator $dec
     */
    public static UpdateOperator dec(String field) {
        return inc(field, -1);
    }

    /**
     * Decrements the value of the field by the specified amount.
     *
     * @param field the field to decrement
     * @param value the number to decrement by
     * @return the update operator
     * @update.operator $dec
     */
    public static UpdateOperator dec(String field, Number value) {
        if ((value instanceof Long) || (value instanceof Integer)) {
            return inc(field, (value.longValue() * -1));
        } else if ((value instanceof Double) || (value instanceof Float)) {
            return inc(field, (value.doubleValue() * -1));
        }
        throw new IllegalArgumentException(Sofia.onlyNumberTypesAllowed());
    }

    /**
     * Increments the value of the field  by one.
     *
     * @param field the field to increment
     * @return the update operator
     * @update.operator $inc
     */
    public static UpdateOperator inc(String field) {
        return inc(field, 1);
    }

    /**
     * Increments the value of the field by the specified amount.
     *
     * @param field the field to increment
     * @param value the number to increment by
     * @return the update operator
     * @update.operator $inc
     */
    public static UpdateOperator inc(String field, Number value) {
        return new UpdateOperator("$inc", field, value);
    }

    /**
     * Only updates the field if the specified value is greater than the existing field value.
     *
     * @param field the field to update
     * @param value the max value to apply
     * @return the update operator
     * @update.operator $max
     */
    public static UpdateOperator max(String field, Number value) {
        return new UpdateOperator("$max", field, value);
    }

    /**
     * Only updates the field if the specified value is greater than the existing field value.
     *
     * @param field the field to update
     * @param value the max value to apply
     * @return the update operator
     * @update.operator $max
     * @since 2.2
     */
    public static UpdateOperator max(String field, Temporal value) {
        return new UpdateOperator("$max", field, value);
    }

    /**
     * Only updates the field if the specified value is greater than the existing field value.
     *
     * @param field the field to update
     * @param value the max value to apply
     * @return the update operator
     * @update.operator $max
     * @since 2.2
     */
    public static UpdateOperator max(String field, Date value) {
        return new UpdateOperator("$max", field, value);
    }

    /**
     * Only updates the field if the specified value is less than the existing field value.
     *
     * @param field the field to update
     * @param value the min value to apply
     * @return the update operator
     * @update.operator $min
     */
    public static UpdateOperator min(String field, Number value) {
        return new UpdateOperator("$min", field, value);
    }

    /**
     * Only updates the field if the specified value is less than the existing field value.
     *
     * @param field the field to update
     * @param value the min value to apply
     * @return the update operator
     * @update.operator $min
     * @since 2.2
     */
    public static UpdateOperator min(String field, Temporal value) {
        return new UpdateOperator("$min", field, value);
    }

    /**
     * Only updates the field if the specified value is less than the existing field value.
     *
     * @param field the field to update
     * @param value the min value to apply
     * @return the update operator
     * @update.operator $min
     * @since 2.2
     */
    public static UpdateOperator min(String field, Date value) {
        return new UpdateOperator("$min", field, value);
    }

    /**
     * Multiplies the value of the field by the specified amount.
     *
     * @param field the field to multiply
     * @param value the number to multiply by
     * @return the update operator
     * @update.operator $mul
     */
    public static UpdateOperator mul(String field, Number value) {
        return new UpdateOperator("$mul", field, value);
    }

    /**
     * The $bit operator performs a bitwise update of a field. The operator supports bitwise and, bitwise or, and bitwise xor (i.e.
     * exclusive or) operations.
     *
     * @param field the field to update
     * @param value the value to use for the update
     * @return the update operator
     * @update.operator $bit
     * @see #and(String, int)
     * @see #xor(String, int)
     */
    public static UpdateOperator or(String field, int value) {
        return new BitOperator("or", field, value);
    }

    /**
     * The $pop operator removes the first or last element of an array. This operation can remove the first of the last value but
     * defaults to removing the last.
     *
     * @param field the field to update
     * @return the update operator
     * @update.operator $pop
     */
    public static PopOperator pop(String field) {
        return new PopOperator(field);
    }

    /**
     * The $pull operator removes from an existing array all instances of a value or values that match a specified condition.
     *
     * @param field  the field to update
     * @param filter the filter to apply
     * @return the update operator
     * @update.operator $pull
     */
    public static UpdateOperator pull(String field, Filter filter) {
        return new PullOperator(field, filter);
    }

    /**
     * The $pullAll operator removes all instances of the specified values from an existing array. Unlike the $pull operator that
     * removes elements by specifying a query, $pullAll removes elements that match the listed values.
     *
     * @param field  the field to update
     * @param values the values to remove
     * @return the update operator
     * @update.operator $pullAll
     */
    public static UpdateOperator pullAll(String field, List<?> values) {
        return new UpdateOperator("$pullAll", field, values);
    }

    /**
     * The $push operator appends a specified value to an array.
     *
     * @param field the field to update
     * @param value the value to add
     * @return the update operator
     * @update.operator $push
     */
    public static PushOperator push(String field, Object value) {
        return new PushOperator(field, singletonList(value));
    }

    /**
     * The $push operator appends a specified value to an array.
     *
     * @param field  the field to update
     * @param values the values to add
     * @return the update operator
     * @update.operator $each
     * @update.operator $push
     */
    public static PushOperator push(String field, List<?> values) {
        return new PushOperator(field, values);
    }

    /**
     * Renames a field.
     *
     * @param field the field to rename
     * @param value the new name
     * @return the update operator
     * @update.operator $rename
     */
    public static UpdateOperator rename(String field, String value) {
        return new UpdateOperator("$rename", field, value);
    }

    /**
     * The $set operator replaces the value of a field with the specified value.
     *
     * @param field the field to set
     * @param value the value to set
     * @return the update operator
     * @update.operator $set
     */
    public static UpdateOperator set(String field, Object value) {
        return new UpdateOperator("$set", field, value);
    }

    /**
     * The $set operator replaces the value of a field with the specified value.
     *
     * @param value the value to set
     * @return the update operator
     * @update.operator $set
     */
    public static UpdateOperator set(Object value) {
        return new SetEntityOperator(value);
    }

    /**
     * If an update operation with upsert: true results in an insert of a document, then $setOnInsert assigns the specified values to
     * the fields in the document. If the update operation does not result in an insert, $setOnInsert does nothing.
     *
     * @param values the fields and their values to insert
     * @return the update operator
     * @update.operator $setOnInsert
     */
    public static UpdateOperator setOnInsert(Map<String, Object> values) {
        return new SetOnInsertOperator(values);
    }

    /**
     * The $unset operator deletes a particular field.
     *
     * @param field the field to set
     * @return the update operator
     * @update.operator $unset
     */
    public static UpdateOperator unset(String field) {
        return new UnsetOperator(field);
    }

    /**
     * The $bit operator performs a bitwise update of a field. The operator supports bitwise and, bitwise or, and bitwise xor (i.e.
     * exclusive or) operations.
     *
     * @param field the field to update
     * @param value the value to use for the update
     * @return the update operator
     * @update.operator $bit
     * @see #and(String, int)
     * @see #or(String, int)
     */
    public static UpdateOperator xor(String field, int value) {
        return new BitOperator("xor", field, value);
    }

}

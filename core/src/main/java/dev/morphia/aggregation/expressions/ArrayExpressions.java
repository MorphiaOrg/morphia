package dev.morphia.aggregation.expressions;

import java.util.List;

import dev.morphia.aggregation.expressions.impls.ArrayIndexExpression;
import dev.morphia.aggregation.expressions.impls.ArrayLiteral;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.FilterExpression;
import dev.morphia.aggregation.expressions.impls.MapExpression;
import dev.morphia.aggregation.expressions.impls.RangeExpression;
import dev.morphia.aggregation.expressions.impls.ReduceExpression;
import dev.morphia.aggregation.expressions.impls.SliceExpression;
import dev.morphia.aggregation.expressions.impls.SortArrayExpression;
import dev.morphia.aggregation.expressions.impls.ZipExpression;
import dev.morphia.annotations.internal.MorphiaExperimental;
import dev.morphia.query.Sort;

import static dev.morphia.aggregation.expressions.Expressions.wrap;

/**
 * Defines helper methods for the array expressions
 *
 * @aggregation.expression Expressions
 * @since 2.0
 */
public final class ArrayExpressions {
    private ArrayExpressions() {
    }

    /**
     * Creates an array of the given objects. This method is an experiment in accepting a wider breadth of types and finding the
     * expressions at encoding time and dealing with them appropriately. There might be bugs in this approach. This method might go away.
     * But it's useful in some Morphia tests, at least.
     *
     * @param objects the objects
     * @return the new expression
     * @since 2.3
     * @morphia.experimental
     */
    @MorphiaExperimental
    public static Expression array(Object... objects) {
        return new ArrayLiteral(objects);
    }

    /**
     * Converts an array of key value pairs to a document.
     *
     * @param array the array to use
     * @return the new expression
     * @aggregation.expression $arrayToObject
     */
    public static Expression arrayToObject(Object array) {
        return new Expression("$arrayToObject", wrap(array));
    }

    /**
     * Concatenates arrays to return the concatenated array.
     *
     * @param array      the array to use
     * @param additional any additional arrays to concatenate
     * @return the new expression
     * @aggregation.expression $concatArrays
     */
    public static Expression concatArrays(Object array, Object... additional) {
        return new Expression("$concatArrays", wrap(array, additional));
    }

    /**
     * Returns the element at the specified array index.
     *
     * @param array the array to use
     * @param index the index to return
     * @return the new expression
     * @aggregation.expression $arrayElemAt
     */
    public static Expression elementAt(Object array, Object index) {
        return new Expression("$arrayElemAt", wrap(List.of(array, index)));
    }

    /**
     * Selects a subset of the array to return an array with only the elements that match the filter condition.
     *
     * @param array       the array to use
     * @param conditional the conditional to use for filtering
     * @return the new expression
     * @aggregation.expression $filter
     */
    public static FilterExpression filter(Expression array, Expression conditional) {
        return Expressions.filter(array, conditional);
    }

    /**
     * Returns a boolean indicating whether a specified value is in an array.
     *
     * @param search the expression to search for
     * @param array  the array to use
     * @return the new expression
     * @aggregation.expression $in
     */
    public static Expression in(Object search, Object array) {
        return new Expression("$in", wrap(List.of(search, array)));
    }

    /**
     * Searches an array for an occurrence of a specified value and returns the array index of the first occurrence. If the substring is not
     * found, returns -1.
     *
     * @param array  the array to use
     * @param search the expression to search for
     * @return the new expression
     * @aggregation.expression $indexOfArray
     */
    public static ArrayIndexExpression indexOfArray(Object array, Object search) {
        return new ArrayIndexExpression(wrap(array), wrap(search));
    }

    /**
     * Determines if the operand is an array. Returns a boolean.
     *
     * @param array the array to use
     * @return the new expression
     * @aggregation.expression $isArray
     */
    public static Expression isArray(Object array) {
        return new Expression("$isArray", wrap(List.of(array)));
    }

    /**
     * Applies a subexpression to each element of an array and returns the array of resulting values in order. Accepts named parameters.
     *
     * @param input the array to use
     * @param in    An expression that is applied to each element of the input array.
     * @return the new expression
     * @aggregation.expression $map
     */
    public static MapExpression map(Object input, Object in) {
        return new MapExpression(input, in);
    }

    /**
     * Converts a document to an array of documents representing key-value pairs.
     *
     * @param array the array to use
     * @return the new expression
     * @aggregation.expression $objectToArray
     */
    public static Expression objectToArray(Object array) {
        return new Expression("$objectToArray", wrap(array));
    }

    /**
     * Outputs an array containing a sequence of integers according to user-defined inputs.
     *
     * @param start the starting value
     * @param end   the ending value
     * @return the new expression
     * @aggregation.expression $range
     */
    public static RangeExpression range(int start, int end) {
        return new RangeExpression(start, end);
    }

    /**
     * Outputs an array containing a sequence of integers according to user-defined inputs.
     *
     * @param start the starting value
     * @param end   the ending value
     * @return the new expression
     * @aggregation.expression $range
     */
    public static RangeExpression range(Object start, Object end) {
        return new RangeExpression(start, end);
    }

    /**
     * Applies an expression to each element in an array and combines them into a single value.
     *
     * @param input   the array to use
     * @param initial The initial cumulative value set before in is applied to the first element of the input array.
     * @param in      A valid expression that $reduce applies to each element in the input array in left-to-right order.
     * @return the new expression
     * @aggregation.expression $reduce
     */
    public static Expression reduce(Object input, Object initial, Object in) {
        return new ReduceExpression(input, initial, in);
    }

    /**
     * Returns an array with the elements in reverse order.
     *
     * @param array the array to use
     * @return the new expression
     * @aggregation.expression $reverseArray
     */
    public static Expression reverseArray(Object array) {
        return new Expression("$reverseArray", wrap(array));
    }

    /**
     * Counts and returns the total number of items in an array.
     *
     * @param array the array to use
     * @return the new expression
     * @aggregation.expression $size
     */
    public static Expression size(Object array) {
        return new Expression("$size", wrap(array));
    }

    /**
     * Returns a subset of an array.
     *
     * @param array the array to use
     * @param size  the number of elements to return
     * @return the new expression
     * @aggregation.expression $slice
     */
    public static Expression slice(Object array, int size) {
        return new SliceExpression(array, size);
    }

    /**
     * Sorts an array based on its elements. To sort by value, use {@link Sort#naturalAscending()} or {@link Sort#naturalDescending()}.
     * See <a href="https://www.mongodb.com/docs/manual/reference/operator/aggregation/sortArray/#sort-by-value">here</a> for details.
     *
     * @param input the array to be sorted.
     * @param sort  the sort order
     * @return the new expression
     * @mongodb.server.release 5.2
     * @aggregation.expression $sortArray
     * @since 2.3
     */
    public static Expression sortArray(Object input, Sort... sort) {
        return new SortArrayExpression(input, sort);
    }

    /**
     * Merge two arrays together.
     *
     * @param arrays the arrays to use
     * @return the new expression
     * @aggregation.expression $zip
     */
    public static ZipExpression zip(Object... arrays) {
        return new ZipExpression(wrap(arrays));
    }

}

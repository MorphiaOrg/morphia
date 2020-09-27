package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.expressions.impls.ArrayExpression;
import dev.morphia.aggregation.experimental.expressions.impls.ArrayFilterExpression;
import dev.morphia.aggregation.experimental.expressions.impls.ArrayIndexExpression;
import dev.morphia.aggregation.experimental.expressions.impls.ArrayLiteral;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.aggregation.experimental.expressions.impls.MapExpression;
import dev.morphia.aggregation.experimental.expressions.impls.RangeExpression;
import dev.morphia.aggregation.experimental.expressions.impls.ReduceExpression;
import dev.morphia.aggregation.experimental.expressions.impls.SliceExpression;
import dev.morphia.aggregation.experimental.expressions.impls.ZipExpression;

import java.util.List;

import static java.util.Arrays.asList;

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
     * Creates an array of the given expressions.  This "expression" isn't so much a mongodb expression as it is a convenience method for
     * building pipeline definitions.
     *
     * @param expressions the expressions
     * @return the new expression
     */
    public static ArrayExpression array(Expression... expressions) {
        return new ArrayLiteral(expressions);
    }

    /**
     * Converts an array of key value pairs to a document.
     *
     * @param array the array to use
     * @return the new expression
     * @aggregation.expression $arrayToObject
     */
    public static Expression arrayToObject(Expression array) {
        return new Expression("$arrayToObject", array);
    }

    /**
     * Concatenates arrays to return the concatenated array.
     *
     * @param array      the array to use
     * @param additional any additional arrays to concatenate
     * @return the new expression
     * @aggregation.expression $concatArrays
     */
    public static Expression concatArrays(Expression array, Expression additional) {
        return new Expression("$concatArrays", asList(array, additional));
    }

    /**
     * Returns the element at the specified array index.
     *
     * @param array the array to use
     * @param index the index to return
     * @return the new expression
     * @aggregation.expression $arrayElemAt
     */
    public static Expression elementAt(Expression array, Expression index) {
        return new Expression("$arrayElemAt", List.of(array, index));
    }

    /**
     * Selects a subset of the array to return an array with only the elements that match the filter condition.
     *
     * @param array       the array to use
     * @param conditional the conditional to use for filtering
     * @return the new expression
     * @aggregation.expression $filter
     */
    public static ArrayFilterExpression filter(Expression array, Expression conditional) {
        return new ArrayFilterExpression(array, conditional);
    }

    /**
     * Returns a boolean indicating whether a specified value is in an array.
     *
     * @param search the expression to search for
     * @param array  the array to use
     * @return the new expression
     * @aggregation.expression $in
     */
    public static Expression in(Expression search, Expression array) {
        return new Expression("$in", List.of(search, array));
    }

    /**
     * Searches an array for an occurrence of a specified value and returns the array index of the first occurernce. If the substring is not
     * found, returns -1.
     *
     * @param array  the array to use
     * @param search the expression to search for
     * @return the new expression
     * @aggregation.expression $indexOfArray
     */
    public static Expression indexOfArray(Expression array, Expression search) {
        return new ArrayIndexExpression(array, search);
    }

    /**
     * Determines if the operand is an array. Returns a boolean.
     *
     * @param array the array to use
     * @return the new expression
     * @aggregation.expression $isArray
     */
    public static Expression isArray(Expression array) {
        return new Expression("$isArray", List.of(array));
    }

    /**
     * Applies a subexpression to each element of an array and returns the array of resulting values in order. Accepts named parameters.
     *
     * @param input the array to use
     * @param in    An expression that is applied to each element of the input array.
     * @return the new expression
     * @aggregation.expression $map
     */
    public static MapExpression map(Expression input, Expression in) {
        return new MapExpression(input, in);
    }

    /**
     * Converts a document to an array of documents representing key-value pairs.
     *
     * @param array the array to use
     * @return the new expression
     * @aggregation.expression $objectToArray
     */
    public static Expression objectToArray(Expression array) {
        return new Expression("$objectToArray", array);
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
     * Applies an expression to each element in an array and combines them into a single value.
     *
     * @param input   the array to use
     * @param initial The initial cumulative value set before in is applied to the first element of the input array.
     * @param in      A valid expression that $reduce applies to each element in the input array in left-to-right order.
     * @return the new expression
     * @aggregation.expression $reduce
     */
    public static Expression reduce(Expression input, Expression initial, Expression in) {
        return new ReduceExpression(input, initial, in);
    }

    /**
     * Returns an array with the elements in reverse order.
     *
     * @param array the array to use
     * @return the new expression
     * @aggregation.expression $reverseArray
     */
    public static Expression reverseArray(Expression array) {
        return new Expression("$reverseArray", array);
    }

    /**
     * Counts and returns the total number of items in an array.
     *
     * @param array the array to use
     * @return the new expression
     * @aggregation.expression $size
     */
    public static Expression size(Expression array) {
        return new Expression("$size", array);
    }

    /**
     * Returns a subset of an array.
     *
     * @param array the array to use
     * @param size  the number of elements to return
     * @return the new expression
     * @aggregation.expression $slice
     */
    public static Expression slice(Expression array, int size) {
        return new SliceExpression(array, size);
    }

    /**
     * Merge two arrays together.
     *
     * @param arrays the arrays to use
     * @return the new expression
     * @aggregation.expression $zip
     */
    public static ZipExpression zip(Expression... arrays) {
        return new ZipExpression(asList(arrays));
    }

}

package dev.morphia.aggregation.expressions;

import java.util.List;

import com.mongodb.lang.Nullable;

import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.ExpressionList;

import static dev.morphia.aggregation.expressions.Expressions.wrap;
import static java.util.Arrays.asList;

/**
 * Defines helper methods for the comparison expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#comparison-expression-operators Comparison Expressions
 * @since 2.0
 */
public final class ComparisonExpressions {
    private ComparisonExpressions() {
    }

    /**
     * Returns 0 if the two values are equivalent, 1 if the first value is greater than the second, and -1 if the first value is less than
     * the second.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $cmp
     */
    public static Expression cmp(Object first, Object second) {
        return new Expression("$cmp", wrap(List.of(first, second)));
    }

    /**
     * Creates an equality check expression
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $eq
     */
    public static Expression eq(Object first, @Nullable Object second) {
        return new Expression("$eq", asList(wrap(first), wrap(second)));
    }

    /**
     * Compares two values and returns:
     *
     * <ol>
     * <li>true when the first value is greater than the second value.
     * <li>false when the first value is less than or equivalent to the second value.
     * </ol>
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $gt
     */
    public static Expression gt(Object first, Object second) {
        return new Expression("$gt", new ExpressionList(wrap(first), wrap(second)));
    }

    /**
     * Compares two values and returns:
     *
     * <ol>
     * <li>true when the first value is greater than or equivalent to the second value.
     * <li>false when the first value is less than the second value.
     * </ol>
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $gte
     */
    public static Expression gte(Object first, Object second) {
        return new Expression("$gte", wrap(List.of(first, second)));
    }

    /**
     * Creates a "less than" comparison.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $lt
     */
    public static Expression lt(Object first, Object second) {
        return new Expression("$lt", wrap(List.of(first, second)));
    }

    /**
     * Compares two values and returns:
     *
     * <ol>
     * <li>true when the first value is less than or equivalent to the second value.
     * <li>false when the first value is greater than the second value.
     * </ol>
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $lte
     */
    public static Expression lte(Object first, Object second) {
        return new Expression("$lte", wrap(List.of(first, second)));
    }

    /**
     * Creates an inequality check expression
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $ne
     */
    public static Expression ne(Object first, @Nullable Object second) {
        return new Expression("$ne", wrap(asList(first, second)));
    }

}

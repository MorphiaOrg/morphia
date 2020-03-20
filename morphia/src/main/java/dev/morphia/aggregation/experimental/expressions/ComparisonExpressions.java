package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.expressions.impls.Expression;

import java.util.List;

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
    public static Expression cmp(final Expression first, final Expression second) {
        return new Expression("$cmp", List.of(first, second));
    }

    /**
     * Compares two values and returns:
     *
     * <li>true when the first value is greater than the second value.
     * <li>false when the first value is less than or equivalent to the second value.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $gt
     */
    public static Expression gt(final Expression first, final Expression second) {
        return new Expression("$gt", List.of(first, second));
    }

    /**
     * Compares two values and returns:
     *
     * <li>true when the first value is greater than or equivalent to the second value.
     * <li>false when the first value is less than the second value.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $gte
     */
    public static Expression gte(final Expression first, final Expression second) {
        return new Expression("$gte", List.of(first, second));
    }

    /**
     * Compares two values and returns:
     *
     * <li>true when the first value is less than or equivalent to the second value.
     * <li>false when the first value is greater than the second value.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $lte
     */
    public static Expression lte(final Expression first, final Expression second) {
        return new Expression("$lte", List.of(first, second));
    }

    /**
     * Returns true if the values are equivalent.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $eq
     */
    public static Expression eq(final Expression first, final Expression second) {
        return new Expression("$eq", List.of(first, second));
    }

    /**
     * Returns true if the first value is less than the second.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $lt
     */
    public static Expression lt(final Expression first, final Expression second) {
        return new Expression("$lt", List.of(first, second));
    }

    /**
     * Returns true if the values are not equivalent.
     *
     * @param first  an expression for the value to compare
     * @param second an expression yielding the value to check against
     * @return the new expression
     * @aggregation.expression $ne
     */
    public static Expression ne(final Expression first, final Expression second) {
        return new Expression("$ne", List.of(first, second));
    }

}

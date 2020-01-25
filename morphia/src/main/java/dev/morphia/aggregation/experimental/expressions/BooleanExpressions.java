package dev.morphia.aggregation.experimental.expressions;

import java.util.List;

import static dev.morphia.aggregation.experimental.expressions.Expression.toList;

/**
 * Defines helper methods for the boolean expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#boolean-expression-operators Boolean Expressions
 * @since 2.0
 */
public final class BooleanExpressions {
    private BooleanExpressions() {
    }

    /**
     * Evaluates one or more expressions and returns true if all of the expressions are true or if evoked with no argument expressions.
     * Otherwise, $and returns false.
     *
     * @param first      the first expression
     * @param additional any additional expressions
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/and $and
     */
    public static Expression and(final Expression first, final Expression... additional) {
        return new Expression("$and", toList(first, additional));
    }

    /**
     * Evaluates one or more expressions and returns true if any of the expressions are true. Otherwise, $or returns false.
     *
     * @param first      the first expression
     * @param additional any additional expressions
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/or $or
     */
    public static Expression or(final Expression first, final Expression... additional) {
        return new Expression("$or", toList(first, additional));
    }

    /**
     * Evaluates a boolean and returns the opposite boolean value; i.e. when passed an expression that evaluates to true, $not returns
     * false; when passed an expression that evaluates to false, $not returns true.
     *
     * @param value the expression
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/not $not
     */
    public static Expression not(final Expression value) {
        return new Expression("$not", List.of(value));
    }

}

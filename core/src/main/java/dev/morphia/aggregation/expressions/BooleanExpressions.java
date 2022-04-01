package dev.morphia.aggregation.expressions;

import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.ExpressionList;

import static dev.morphia.aggregation.expressions.impls.ExpressionList.coalesce;

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
     * @aggregation.expression $and
     */
    public static Expression and(Expression first, Expression... additional) {
        return new Expression("$and", coalesce(first, additional));
    }

    /**
     * Evaluates a boolean and returns the opposite boolean value; i.e. when passed an expression that evaluates to true, $not returns
     * false; when passed an expression that evaluates to false, $not returns true.
     *
     * @param value the expression
     * @return the new expression
     * @aggregation.expression $not
     */
    public static Expression not(Expression value) {
        return new Expression("$not", new ExpressionList(value));
    }

    /**
     * Evaluates one or more expressions and returns true if any of the expressions are true. Otherwise, $or returns false.
     *
     * @param first      the first expression
     * @param additional any additional expressions
     * @return the new expression
     * @aggregation.expression $or
     */
    public static Expression or(Expression first, Expression... additional) {
        return new Expression("$or", coalesce(first, additional));
    }

}

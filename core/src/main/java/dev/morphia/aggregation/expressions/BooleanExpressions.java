package dev.morphia.aggregation.expressions;

import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.ExpressionList;
import dev.morphia.aggregation.expressions.impls.LogicalExpression;

import static dev.morphia.aggregation.expressions.Expressions.wrap;

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
     * Evaluates one or more values and returns true if all the values are true or if evoked with no argument expressions.
     * Otherwise, $and returns false.
     *
     * @param first      the first value
     * @param additional any additional values
     * @return the new expression
     * @aggregation.expression $and
     */
    public static LogicalExpression and(Object first, Object... additional) {
        return new LogicalExpression("$and", new ExpressionList(wrap(first, additional)));
    }

    /**
     * Evaluates one or more values and returns true if all the values are true or if evoked with no argument expressions.
     * Otherwise, $and returns false.
     *
     * @return the new expression
     * @aggregation.expression $and
     * @since 2.3
     */
    public static LogicalExpression and() {
        return new LogicalExpression("$and");
    }

    /**
     * Evaluates a boolean and returns the opposite boolean value; i.e. when passed an expression that evaluates to true, $not returns
     * false; when passed an expression that evaluates to false, $not returns true.
     *
     * @param value the expression
     * @return the new expression
     * @aggregation.expression $not
     */
    public static Expression not(Object value) {
        return new Expression("$not", new ExpressionList(wrap(value)));
    }

    /**
     * Evaluates one or more values and returns true if any of the values are true. Otherwise, $or returns false.
     *
     * @param first      the first value
     * @param additional any additional values
     * @return the new expression
     * @aggregation.expression $or
     */
    public static LogicalExpression or(Object first, Object... additional) {
        return new LogicalExpression("$or", new ExpressionList(wrap(first, additional)));
    }

    /**
     * Evaluates one or more values and returns true if any of the values are true. Otherwise, $or returns false.
     *
     * @return the new expression
     * @aggregation.expression $or
     * @since 2.3
     */
    public static LogicalExpression or() {
        return new LogicalExpression("$or");
    }

}

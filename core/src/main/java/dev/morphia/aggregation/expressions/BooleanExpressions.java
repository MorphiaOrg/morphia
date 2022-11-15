package dev.morphia.aggregation.expressions;

import com.mongodb.lang.NonNull;

import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.ExpressionList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
     * Evaluates one or more expressions and returns true if all the expressions are true or if evoked with no argument expressions.
     * Otherwise, $and returns false.
     *
     * @param first      the first expression
     * @param additional any additional expressions
     * @return the new expression
     * @aggregation.expression $and
     */
    public static LogicalExpression and(Expression first, Expression... additional) {
        return new LogicalExpression("$and", coalesce(first, additional));
    }

    /**
     * Evaluates one or more expressions and returns true if all the expressions are true or if evoked with no argument expressions.
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
    public static LogicalExpression or(Expression first, Expression... additional) {
        return new LogicalExpression("$or", coalesce(first, additional));
    }

    /**
     * Evaluates one or more expressions and returns true if any of the expressions are true. Otherwise, $or returns false.
     *
     * @return the new expression
     * @aggregation.expression $or
     * @since 2.3
     */
    public static LogicalExpression or() {
        return new LogicalExpression("$or");
    }

    /**
     * Defines a logical expression.
     *
     * @since 2.3
     */
    public static class LogicalExpression extends Expression {
        private LogicalExpression(String operation) {
            super(operation, new ExpressionList());
        }

        private LogicalExpression(String operation, @NonNull ExpressionList list) {
            super(operation, list);
        }

        /**
         * Adds a new expression to this LogicalExpression.
         *
         * @param expression the new expression
         * @return this
         * @since 2.3
         */
        @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
        public LogicalExpression add(Expression expression) {
            ((ExpressionList) getValue()).add(expression);
            return this;
        }
    }
}

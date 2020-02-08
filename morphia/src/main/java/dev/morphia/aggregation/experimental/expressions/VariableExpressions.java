package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.aggregation.experimental.expressions.impls.LetExpression;

/**
 * Defines helper methods for the variable expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#variable-expression-operators Variable Expressions
 * @since 2.0
 */
public final class VariableExpressions {
    private VariableExpressions() {
    }

    /**
     * Binds variables for use in the specified expression, and returns the result of the expression.
     *
     * @param in the expression to evaluate.  variables can be defined using the {@link LetExpression#variable(String, Expression)} method
     * @return the new expression
     * @aggregation.expression $let
     */
    public static LetExpression let(final Expression in) {
        return new LetExpression(in);
    }
}

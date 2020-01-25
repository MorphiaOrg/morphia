package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.aggregation.experimental.expressions.impls.IfNull;

import java.util.List;

/**
 * Defines helper methods for the conditional expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#conditional-expression-operators Conditional Expressions
 * @since 2.0
 */
public class ConditionalExpressions {
    private ConditionalExpressions() {
    }

    /**
     * Evaluates a boolean expression to return one of the two specified return expressions.
     *
     * @param condition the condition to evaluate
     * @param then      the expression for the true branch
     * @param otherwise the expresion for the else branch
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/cond $cond
     */
    public static Expression condition(final Expression condition, final Expression then, final Expression otherwise) {
        return new Expression("$cond", List.of(condition, then, otherwise));
    }

    /**
     * Evaluates an expression and returns the value of the expression if the expression evaluates to a non-null value. If the
     * expression evaluates to a null value, including instances of undefined values or missing fields, returns the value of the
     * replacement expression.
     *
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/ifNull $ifNull
     */
    public static IfNull ifNull() {
        return new IfNull();
    }

}

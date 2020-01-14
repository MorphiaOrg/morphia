package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.expressions.internal.IfNull;

import java.util.List;

/**
 * Base class for the conditional expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#conditional-expression-operators Conditional Expressions
 */
public class ConditionalExpression extends Expression {
    protected ConditionalExpression(final String operation) {
        super(operation);
    }

    protected ConditionalExpression(final String operation, final Object value) {
        super(operation, value);
    }

    /**
     * Evaluates a boolean expression to return one of the two specified return expressions.
     *
     * @param condition the condition to evaluate
     * @param then      the expression for the true branch
     * @param otherwise the expresion for the else branch
     * @return the new expression
     * @mongodb.driver.manual manual/reference/operator/aggregation/if $if
     */
    public static ConditionalExpression condition(final Expression condition, final Expression then, final Expression otherwise) {
        return new ConditionalExpression("$cond", List.of(condition, then, otherwise));
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

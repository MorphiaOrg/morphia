package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.NonNull;

import dev.morphia.annotations.internal.MorphiaInternal;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Defines a logical expression.
 *
 * @since 2.3
 */
public class LogicalExpression extends Expression {
    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public LogicalExpression(String operation) {
        super(operation, new ExpressionList());
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public LogicalExpression(String operation, @NonNull ExpressionList list) {
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
        ((ExpressionList) value()).add(expression);
        return this;
    }
}

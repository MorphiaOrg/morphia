package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.NonNull;

import dev.morphia.annotations.internal.MorphiaInternal;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static dev.morphia.aggregation.expressions.Expressions.wrap;

/**
 * Defines a logical expression.
 *
 * @since 2.3
 */
public class LogicalExpression extends Expression {
    /**
     * @param operation the operation name
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public LogicalExpression(String operation) {
        super(operation, new ExpressionList());
    }

    /**
     * @param operation the operation name
     * @param list      the expressions to evaluate
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public LogicalExpression(String operation, @NonNull ExpressionList list) {
        super(operation, list);
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the value as an ExpressionList
     */
    @NonNull
    @Override
    @MorphiaInternal
    @SuppressWarnings("DataFlowIssue")
    public ExpressionList value() {
        return (ExpressionList) super.value();
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
        value().add(expression);
        return this;
    }

    /**
     * Adds a new expression to this LogicalExpression.
     *
     * @param value the new expression
     * @return this
     * @since 2.3
     */
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public LogicalExpression add(boolean value) {
        value().add(wrap(value));
        return this;
    }
}

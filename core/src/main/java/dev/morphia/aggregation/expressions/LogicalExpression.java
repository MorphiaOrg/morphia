package dev.morphia.aggregation.expressions;

import com.mongodb.lang.NonNull;

import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.ExpressionList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Defines a logical expression.
 *
 * @since 2.3
 */
public class LogicalExpression extends Expression {
    LogicalExpression(String operation) {
        super(operation, new ExpressionList());
    }

    LogicalExpression(String operation, @NonNull ExpressionList list) {
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

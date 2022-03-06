package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.aggregation.experimental.expressions.impls.ShiftExpression;
import dev.morphia.aggregation.experimental.stages.SetWindowFields;

/**
 * Provides window specific operations.
 *
 * @since 2.3
 */
public final class WindowExpressions {
    private WindowExpressions() {
    }

    /**
     * Returns the value from an expression applied to a document in a specified position relative to the current document in the
     * $setWindowFields stage partition.
     *
     * @param output       Specifies an expression to evaluate and return in the output.
     * @param by           Specifies an integer with a numeric document position relative to the current document in the output.
     * @param defaultValue Specifies an optional default expression to evaluate if the document position is outside of the implicit
     *                     $setWindowFields stage window. The implicit window contains all the documents in the partition.
     * @return the expression
     * @aggregation.expression $shift
     * @mongodb.server.release 5.0
     * @see dev.morphia.aggregation.experimental.Aggregation#setWindowFields(SetWindowFields)
     * @since 2.3
     */
    public static ShiftExpression shift(Expression output, long by, Expression defaultValue) {
        return new ShiftExpression(output, by, defaultValue);
    }
}

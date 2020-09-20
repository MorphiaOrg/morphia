package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.expressions.impls.Expression;

/**
 * Defines helper methods for the data size expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#data-size-operators Data Size Expressions
 * @since 2.1
 */
public final class DataSizeExpressions {
    private DataSizeExpressions() {
    }

    /**
     * Returns the size of a given string or binary data value’s content in bytes.
     *
     * @param expression the binary size expression
     */
    public static Expression binarySize(Expression expression) {
        return new Expression("$binarySize", expression);
    }
}

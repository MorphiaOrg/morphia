package dev.morphia.aggregation.expressions;

import dev.morphia.aggregation.expressions.impls.Expression;

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
     * @return the new expression
     * @aggregation.expression $binarySize
     */
    public static Expression binarySize(Expression expression) {
        return new Expression("$binarySize", expression);
    }

    /**
     * Returns the size in bytes of a given document (i.e. bsontype Object) when encoded as BSON.
     *
     * @param expression the bson size expression
     * @return the new expression
     * @aggregation.expression $bsonSize
     */
    public static Expression bsonSize(Expression expression) {
        return new Expression("$bsonSize", expression);
    }
}

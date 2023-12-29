package dev.morphia.aggregation.expressions;

import dev.morphia.aggregation.expressions.impls.Expression;

import static dev.morphia.aggregation.expressions.Expressions.wrap;

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
     * @param value the value to examine
     * @return the new value
     * @aggregation.expression $binarySize
     * @mongodb.server.release 4.4
     */
    public static Expression binarySize(Object value) {
        return new Expression("$binarySize", wrap(value));
    }

    /**
     * Returns the size in bytes of a given document (i.e. bsontype Object) when encoded as BSON.
     *
     * @param value the value to examine
     * @return the new expression
     * @aggregation.expression $bsonSize
     * @mongodb.server.release 4.4
     */
    public static Expression bsonSize(Object value) {
        return new Expression("$bsonSize", wrap(value));
    }
}

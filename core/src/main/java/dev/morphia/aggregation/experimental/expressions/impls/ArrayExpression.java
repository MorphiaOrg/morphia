package dev.morphia.aggregation.experimental.expressions.impls;

import com.mongodb.lang.Nullable;

/**
 * Base class for the array expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#array-expression-operators Array Expressions
 */
public class ArrayExpression extends Expression {
    public ArrayExpression(String operation, @Nullable Object value) {
        super(operation, value);
    }
}

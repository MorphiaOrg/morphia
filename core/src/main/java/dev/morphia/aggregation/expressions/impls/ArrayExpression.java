package dev.morphia.aggregation.expressions.impls;

import java.util.List;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Base class for the array expressions
 *
 * @hidden
 * @mongodb.driver.manual reference/operator/aggregation/#array-expression-operators Array Expressions
 */
@MorphiaInternal
public class ArrayExpression extends Expression implements SingleValuedExpression {
    /**
     * @param operation the operation name
     * @param value     the array contents
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ArrayExpression(String operation, List<Expression> value) {
        super(operation, value);
    }
}

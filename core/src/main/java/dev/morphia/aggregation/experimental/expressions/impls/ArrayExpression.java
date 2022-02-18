package dev.morphia.aggregation.experimental.expressions.impls;

import java.util.List;

/**
 * Base class for the array expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#array-expression-operators Array Expressions
 */
public class ArrayExpression extends Expression implements SingleValuedExpression {
    public ArrayExpression(String operation, List<Expression> value) {
        super(operation, value);
    }
}

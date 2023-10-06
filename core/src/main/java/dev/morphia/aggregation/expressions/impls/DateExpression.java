package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Base class for the date expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#date-expression-operators Date Expressions
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class DateExpression extends Expression {
    /**
     * @param operation the operation name
     * @param value     the value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public DateExpression(String operation, Expression value) {
        super(operation, value);
    }
}

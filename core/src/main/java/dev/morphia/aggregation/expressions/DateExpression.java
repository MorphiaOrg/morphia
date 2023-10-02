package dev.morphia.aggregation.expressions;

import dev.morphia.aggregation.expressions.impls.Expression;

/**
 * Base class for the date expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#date-expression-operators Date Expressions
 */
public class DateExpression extends Expression {
    protected DateExpression(String operation, Expression value) {
        super(operation, value);
    }
}

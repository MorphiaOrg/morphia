package dev.morphia.aggregation.expressions.impls;

public class MedianExpression extends Expression {
    public MedianExpression(Expression input) {
        super("$median", input);
    }
}

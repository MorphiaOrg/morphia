package dev.morphia.aggregation.expressions.impls;

/**
 * @hidden
 * @since 3.0
 */
public class MedianExpression extends Expression {
    public MedianExpression(Expression input) {
        super("$median", input);
    }
}

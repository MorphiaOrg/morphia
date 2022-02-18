package dev.morphia.aggregation.experimental.expressions.impls;

public class LiteralExpression extends Expression {
    public LiteralExpression(Object value) {
        super("$literal", value);
    }
}

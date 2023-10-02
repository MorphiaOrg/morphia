package dev.morphia.aggregation.expressions.impls;

public class TrimExpression extends Expression {
    private final Expression input;
    private Expression chars;

    public TrimExpression(String operator, Expression input) {
        super(operator);
        this.input = input;
    }

    public TrimExpression chars(Expression chars) {
        this.chars = chars;
        return this;
    }

    public Expression input() {
        return input;
    }

    public Expression chars() {
        return chars;
    }
}

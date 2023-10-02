package dev.morphia.aggregation.expressions.impls;

public class ReduceExpression extends Expression {
    private final Expression input;
    private final Expression initial;
    private final Expression in;

    public ReduceExpression(Expression input, Expression initial, Expression in) {
        super("$reduce");
        this.input = input;
        this.initial = initial;
        this.in = in;
    }

    public Expression input() {
        return input;
    }

    public Expression initial() {
        return initial;
    }

    public Expression in() {
        return in;
    }
}

package dev.morphia.aggregation.expressions.impls;

public class UnsetFieldExpression extends Expression {
    private final Expression field;
    private final Object input;

    public UnsetFieldExpression(Expression field, Object input) {
        super("$unsetField");
        this.field = field;
        this.input = input;
    }

    public Expression field() {
        return field;
    }

    public Object input() {
        return input;
    }
}

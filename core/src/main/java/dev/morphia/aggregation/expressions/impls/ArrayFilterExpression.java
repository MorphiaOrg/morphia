package dev.morphia.aggregation.expressions.impls;

public class ArrayFilterExpression extends Expression {
    private final Expression array;
    private final Expression conditional;
    private ValueExpression as;

    public ArrayFilterExpression(Expression array, Expression conditional) {
        super("$filter");
        this.array = array;
        this.conditional = conditional;
    }

    public ArrayFilterExpression as(String as) {
        this.as = new ValueExpression(as);
        return this;
    }

    public Expression array() {
        return array;
    }

    public Expression conditional() {
        return conditional;
    }

    public ValueExpression as() {
        return as;
    }
}

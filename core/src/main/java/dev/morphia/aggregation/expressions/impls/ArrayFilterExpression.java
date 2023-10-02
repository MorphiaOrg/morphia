package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

public class ArrayFilterExpression extends Expression {
    private final Expression array;
    private final Expression conditional;
    private ValueExpression as;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ArrayFilterExpression(Expression array, Expression conditional) {
        super("$filter");
        this.array = array;
        this.conditional = conditional;
    }

    public ArrayFilterExpression as(String as) {
        this.as = new ValueExpression(as);
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression array() {
        return array;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression conditional() {
        return conditional;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ValueExpression as() {
        return as;
    }
}

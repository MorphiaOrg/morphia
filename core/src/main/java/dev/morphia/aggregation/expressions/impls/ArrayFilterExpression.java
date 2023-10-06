package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Selects a subset of the array to return an array with only the elements that match the filter condition.
 *
 * @aggregation.expression $filter
 */
public class ArrayFilterExpression extends Expression {
    private final Expression array;
    private final Expression conditional;
    private ValueExpression as;

    /**
     * @param array       the array express
     * @param conditional the conditional to apply
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ArrayFilterExpression(Expression array, Expression conditional) {
        super("$filter");
        this.array = array;
        this.conditional = conditional;
    }

    /**
     * @param as the as expression
     * @return this
     */
    public ArrayFilterExpression as(String as) {
        this.as = new ValueExpression(as);
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the array expression
     */
    @MorphiaInternal
    public Expression array() {
        return array;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the conditional
     */
    @MorphiaInternal
    public Expression conditional() {
        return conditional;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the as expression
     */
    @MorphiaInternal
    public ValueExpression as() {
        return as;
    }
}

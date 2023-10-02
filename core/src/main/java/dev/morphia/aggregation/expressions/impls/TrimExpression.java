package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

public class TrimExpression extends Expression {
    private final Expression input;
    private Expression chars;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public TrimExpression(String operator, Expression input) {
        super(operator);
        this.input = input;
    }

    public TrimExpression chars(Expression chars) {
        this.chars = chars;
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression input() {
        return input;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression chars() {
        return chars;
    }
}

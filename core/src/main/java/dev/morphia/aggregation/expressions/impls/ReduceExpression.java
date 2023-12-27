package dev.morphia.aggregation.expressions.impls;

import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class ReduceExpression extends Expression {
    private final Expression input;
    private final Expression initial;
    private final Expression in;

    /**
     * @param input   the input
     * @param initial the initial value
     * @param in      the in expression
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ReduceExpression(Object input, Object initial, Object in) {
        super("$reduce");
        this.input = Expressions.wrap(input);
        this.initial = Expressions.wrap(initial);
        this.in = Expressions.wrap(in);
    }

    /**
     * @return the input
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
     * @return the initial
     */
    @MorphiaInternal
    public Expression initial() {
        return initial;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the in
     */
    @MorphiaInternal
    public Expression in() {
        return in;
    }
}

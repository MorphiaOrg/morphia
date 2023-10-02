package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Gives first/last results
 * 
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class EndResultsExpression extends Expression {
    private final Expression input;
    private final Expression n;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public EndResultsExpression(String operation, Expression n, Expression input) {
        super(operation);
        this.input = input;
        this.n = n;
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
    public Expression n() {
        return n;
    }
}

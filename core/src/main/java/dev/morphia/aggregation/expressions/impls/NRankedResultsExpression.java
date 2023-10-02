package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.query.Sort;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class NRankedResultsExpression extends Expression {
    private final Expression output;
    private final Expression n;
    private final Sort[] sortBy;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public NRankedResultsExpression(String operation, Expression n, Expression output, Sort... sortBy) {
        super(operation);
        this.output = output;
        this.n = n;
        this.sortBy = sortBy;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression output() {
        return output;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression n() {
        return n;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Sort[] sortBy() {
        return sortBy;
    }
}

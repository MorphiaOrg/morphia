package dev.morphia.aggregation.expressions.impls;

import dev.morphia.aggregation.expressions.Expressions;
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
     * @param operation the operation name
     * @param n         the n expression
     * @param output    the output expression
     * @param sortBy    the sort
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public NRankedResultsExpression(String operation, Object n, Object output, Sort... sortBy) {
        super(operation);
        this.output = Expressions.wrap(output);
        this.n = Expressions.wrap(n);
        this.sortBy = sortBy;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the output
     */
    @MorphiaInternal
    public Expression output() {
        return output;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the n expression
     */
    @MorphiaInternal
    public Expression n() {
        return n;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the sort
     */
    @MorphiaInternal
    public Sort[] sortBy() {
        return sortBy;
    }
}

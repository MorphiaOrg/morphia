package dev.morphia.aggregation.expressions.impls;

import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.query.Sort;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class RankedResultsExpression extends Expression {
    private final Expression output;
    private final Sort[] sortBy;

    /**
     * @param operation the operation
     * @param output    the output
     * @param sortBy    the sort
     */
    public RankedResultsExpression(String operation, Object output, Sort... sortBy) {
        super(operation);
        this.output = Expressions.wrap(output);
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
     * @return the sort
     */
    @MorphiaInternal
    public Sort[] sortBy() {
        return sortBy;
    }
}

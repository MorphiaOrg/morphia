package dev.morphia.aggregation.expressions.impls;

import dev.morphia.query.Sort;

public class RankedResultsExpression extends Expression {
    private final Expression output;
    private final Sort[] sortBy;

    public RankedResultsExpression(String operation, Expression output, Sort... sortBy) {
        super(operation);
        this.output = output;
        this.sortBy = sortBy;
    }

    public Expression output() {
        return output;
    }

    public Sort[] sortBy() {
        return sortBy;
    }
}

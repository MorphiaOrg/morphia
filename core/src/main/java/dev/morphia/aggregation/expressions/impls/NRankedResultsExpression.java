package dev.morphia.aggregation.expressions.impls;

import dev.morphia.query.Sort;

public class NRankedResultsExpression extends Expression {
    private final Expression output;
    private final Expression n;
    private final Sort[] sortBy;

    public NRankedResultsExpression(String operation, Expression n, Expression output, Sort... sortBy) {
        super(operation);
        this.output = output;
        this.n = n;
        this.sortBy = sortBy;
    }

    public Expression output() {
        return output;
    }

    public Expression n() {
        return n;
    }

    public Sort[] sortBy() {
        return sortBy;
    }
}

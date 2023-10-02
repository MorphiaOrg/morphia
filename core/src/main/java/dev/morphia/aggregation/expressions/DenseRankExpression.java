package dev.morphia.aggregation.expressions;

import dev.morphia.aggregation.expressions.impls.Expression;

public class DenseRankExpression extends Expression {
    public DenseRankExpression() {
        super("$denseRank");
    }
}

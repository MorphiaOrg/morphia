package dev.morphia.aggregation.expressions;

import dev.morphia.aggregation.expressions.impls.Expression;

public class RankExpression extends Expression {
    public RankExpression() {
        super("$rank");
    }
}

package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class DenseRankExpression extends Expression {
    public DenseRankExpression() {
        super("$denseRank");
    }
}

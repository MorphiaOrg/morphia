package dev.morphia.aggregation.expressions.impls;

import java.util.List;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Base class for the accumulator expression types.
 *
 * @hidden
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
public class Accumulator extends Expression {

    /**
     * @param operation the operation name
     * @param values    the values
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Accumulator(String operation, List<Expression> values) {
        super(operation, new ExpressionList(values));
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public ExpressionList value() {
        return (ExpressionList) super.value();
    }
}

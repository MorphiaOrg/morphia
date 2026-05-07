package dev.morphia.aggregation.expressions.impls;

import java.util.List;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @since 3.0
 */
public class PercentileExpression extends Expression {
    private final List<Expression> inputs;

    private final List<Expression> percentiles;

    /**
     * @param inputs      the input expressions
     * @param percentiles the percentile expressions
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public PercentileExpression(List<Expression> inputs, List<Expression> percentiles) {
        super("$percentile");
        this.inputs = inputs;
        this.percentiles = percentiles;
    }

    /**
     * @return the input expressions
     * @hidden
     */
    public List<Expression> inputs() {
        return inputs;
    }

    /**
     * @return the percentile expressions
     * @hidden
     */
    public List<Expression> percentiles() {
        return percentiles;
    }
}

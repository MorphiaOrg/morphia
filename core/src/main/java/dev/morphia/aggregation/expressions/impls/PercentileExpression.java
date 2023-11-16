package dev.morphia.aggregation.expressions.impls;

import java.util.List;

/**
 * @hidden
 * @since 3.0
 */
public class PercentileExpression extends Expression {
    private final List<Expression> inputs;

    private final List<Expression> percentiles;

    public PercentileExpression(List<Expression> inputs, List<Expression> percentiles) {
        super("$percentile");
        this.inputs = inputs;
        this.percentiles = percentiles;
    }

    /**
     * @hidden
     * @return
     */
    public List<Expression> inputs() {
        return inputs;
    }

    /**
     * @hidden
     * @return
     */
    public List<Expression> percentiles() {
        return percentiles;
    }
}

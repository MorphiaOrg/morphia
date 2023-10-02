package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;

public class ExpMovingAvg extends Expression {
    private final Expression input;
    private final Integer n;
    private final Double alpha;

    public ExpMovingAvg(Expression input, int n) {
        super("$expMovingAvg");
        this.input = input;
        this.n = n;
        alpha = null;
    }

    public ExpMovingAvg(Expression input, double alpha) {
        super("$expMovingAvg");
        this.input = input;
        this.n = null;
        this.alpha = alpha;
    }

    public Expression input() {
        return input;
    }

    @Nullable
    public Integer n() {
        return n;
    }

    public Double alpha() {
        return alpha;
    }
}

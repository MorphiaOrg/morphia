package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class ExpMovingAvg extends Expression {
    private final Expression input;
    private final Integer n;
    private final Double alpha;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ExpMovingAvg(Expression input, int n) {
        super("$expMovingAvg");
        this.input = input;
        this.n = n;
        alpha = null;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ExpMovingAvg(Expression input, double alpha) {
        super("$expMovingAvg");
        this.input = input;
        this.n = null;
        this.alpha = alpha;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression input() {
        return input;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Nullable
    public Integer n() {
        return n;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Double alpha() {
        return alpha;
    }
}

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
     * @param input the input
     * @param n     the n value
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
     * @param input the input
     * @param alpha the alpha value
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
     * @return the input
     */
    @MorphiaInternal
    public Expression input() {
        return input;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the n value
     */
    @MorphiaInternal
    @Nullable
    public Integer n() {
        return n;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the alpha value
     */
    @Nullable
    @MorphiaInternal
    public Double alpha() {
        return alpha;
    }
}

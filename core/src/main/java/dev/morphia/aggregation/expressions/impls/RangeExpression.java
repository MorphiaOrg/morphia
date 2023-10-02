package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

public class RangeExpression extends Expression {
    private final int start;
    private final int end;
    private Integer step;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public RangeExpression(int start, int end) {
        super("$range");
        this.start = start;
        this.end = end;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public int start() {
        return start;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public int end() {
        return end;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Nullable
    public Integer step() {
        return step;
    }

    public RangeExpression step(Integer step) {
        this.step = step;
        return this;
    }
}

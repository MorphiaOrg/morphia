package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Outputs an array containing a sequence of integers according to user-defined inputs.
 */
public class RangeExpression extends Expression {
    private final int start;
    private final int end;
    private Integer step;

    /**
     * @param start the start
     * @param end   the end
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
     * @return the start
     */
    @MorphiaInternal
    public int start() {
        return start;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the end
     */
    @MorphiaInternal
    public int end() {
        return end;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the step size
     */
    @MorphiaInternal
    @Nullable
    public Integer step() {
        return step;
    }

    /**
     * @param step the step size
     * @return this
     */
    public RangeExpression step(Integer step) {
        this.step = step;
        return this;
    }
}

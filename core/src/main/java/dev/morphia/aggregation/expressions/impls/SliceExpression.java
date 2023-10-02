package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

public class SliceExpression extends Expression {
    private final Expression array;
    private final int size;
    private Integer position;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public SliceExpression(Expression array, int size) {
        super("$slice");
        this.array = array;
        this.size = size;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression array() {
        return array;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public int size() {
        return size;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Integer position() {
        return position;
    }

    public SliceExpression position(Integer position) {
        this.position = position;
        return this;
    }
}

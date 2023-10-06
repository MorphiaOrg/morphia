package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Returns a subset of an array.
 */
public class SliceExpression extends Expression {
    private final Expression array;
    private final int size;
    private Integer position;

    /**
     * @param array the array to slice
     * @param size  the slice size
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
     * @return the array
     */
    @MorphiaInternal
    public Expression array() {
        return array;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the slice size
     */
    @MorphiaInternal
    public int size() {
        return size;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the position
     */
    @MorphiaInternal
    public Integer position() {
        return position;
    }

    /**
     * @param position the starting position of the slice
     * @return this
     */
    public SliceExpression position(Integer position) {
        this.position = position;
        return this;
    }
}

package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @since 2.0
 */
public class ArrayIndexExpression extends Expression {
    private final Expression array;
    private final Expression search;
    private Integer start;
    private Integer end;

    /**
     * @param array
     * @param search
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ArrayIndexExpression(Expression array, Expression search) {
        super("$indexOfArray");
        this.array = array;
        this.search = search;
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
    public Expression search() {
        return search;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Nullable
    public Integer start() {
        return start;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Nullable
    public Integer end() {
        return end;
    }

    /**
     * The ending index
     *
     * @param end the ending index
     * @return this
     */
    public ArrayIndexExpression end(Integer end) {
        this.end = end;
        return this;
    }

    /**
     * The starting index
     *
     * @param start the starting index
     * @return this
     */
    public ArrayIndexExpression start(Integer start) {
        this.start = start;
        return this;
    }
}

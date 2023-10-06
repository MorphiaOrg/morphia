package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Searches an array for an occurrence of a specified value and returns the array index of the first occurrence. If the substring is not
 * found, returns -1.
 * 
 * @since 2.0
 */
public class ArrayIndexExpression extends Expression {
    private final Expression array;
    private final Expression search;
    private Integer start;
    private Integer end;

    /**
     * @param array  the array expression
     * @param search the search expression
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
     * @return the array expression
     */
    @MorphiaInternal
    public Expression array() {
        return array;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the search expression
     */
    @MorphiaInternal
    public Expression search() {
        return search;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the start index
     */
    @MorphiaInternal
    @Nullable
    public Integer start() {
        return start;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the end index
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

package dev.morphia.aggregation.expressions.impls;

import java.util.List;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Merge two arrays together.
 */
public class ZipExpression extends Expression {
    private final List<Expression> inputs;
    private ValueExpression useLongestLength;
    private Expression defaults;

    /**
     * @param inputs the inputs
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ZipExpression(List<Expression> inputs) {
        super("$zip");
        this.inputs = inputs;
    }

    /**
     * An array of default element values to use if the input arrays have different lengths. You must specify useLongestLength: true
     * along with this field, or else $zip will return an error.
     * <p>
     * If useLongestLength: true but defaults is empty or not specified, $zip uses null as the default value.
     * <p>
     * If specifying a non-empty defaults, you must specify a default for each input array or else $zip will return an error.
     *
     * @param defaults the defaults
     * @return this
     */
    public ZipExpression defaults(Expression defaults) {
        this.defaults = defaults;
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the inputs
     */
    @MorphiaInternal
    public List<Expression> inputs() {
        return inputs;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return use longest length
     */
    @MorphiaInternal
    public ValueExpression useLongestLength() {
        return useLongestLength;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the defaults
     */
    @MorphiaInternal
    public Expression defaults() {
        return defaults;
    }

    /**
     * Specifies whether the length of the longest array determines the number of arrays in the output array. The default on the server
     * is false.
     *
     * @param useLongestLength true to use the longest length
     * @return this
     */
    public ZipExpression useLongestLength(Boolean useLongestLength) {
        this.useLongestLength = new ValueExpression(useLongestLength);
        return this;
    }
}

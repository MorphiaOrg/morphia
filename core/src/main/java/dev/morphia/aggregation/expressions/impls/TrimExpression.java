package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Removes whitespace or the specified characters from a string.
 */
public class TrimExpression extends Expression {
    private final Expression input;
    private Expression chars;

    /**
     * @param operator the operator name
     * @param input    the input
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public TrimExpression(String operator, Expression input) {
        super(operator);
        this.input = input;
    }

    /**
     * @param chars The character(s) to trim from input.
     * @return this
     */
    public TrimExpression chars(Expression chars) {
        this.chars = chars;
        return this;
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
     * @return the chars
     */
    @MorphiaInternal
    public Expression chars() {
        return chars;
    }
}

package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Converts a value to a specified type.
 */
public class ConvertExpression extends Expression {
    private final Expression input;
    private final ConvertType to;
    private Expression onError;
    private Expression onNull;

    /**
     * @param input the input expression
     * @param to    the target type
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ConvertExpression(Expression input, ConvertType to) {
        super("$convert");
        this.input = input;
        this.to = to;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the input expression
     */
    @MorphiaInternal
    public Expression input() {
        return input;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the target type
     */
    @MorphiaInternal
    public ConvertType to() {
        return to;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the onError expression
     */
    @MorphiaInternal
    public Expression onError() {
        return onError;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the onNull expression
     */
    @MorphiaInternal
    public Expression onNull() {
        return onNull;
    }

    /**
     * The value to return on encountering an error during conversion, including unsupported type conversions.
     *
     * @param onError the value
     * @return this
     */
    public ConvertExpression onError(Expression onError) {
        this.onError = onError;
        return this;
    }

    /**
     * The value to return if the input is null or missing.
     *
     * @param onNull the value
     * @return this
     */
    public ConvertExpression onNull(Expression onNull) {
        this.onNull = onNull;
        return this;
    }
}

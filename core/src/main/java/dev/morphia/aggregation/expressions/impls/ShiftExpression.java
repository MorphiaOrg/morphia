package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @aggregation.expression $shift
 * @mongodb.server.release 5.0
 * @since 2.3
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class ShiftExpression extends Expression {
    private final Expression output;
    private final long by;
    private final Expression defaultValue;

    /**
     * @param output the output
     * @param by     the amount to shift by
     */
    public ShiftExpression(Expression output, long by) {
        super("$shift");
        this.output = output;
        this.by = by;
        this.defaultValue = null;
    }

    /**
     * @param output       the output
     * @param by           the amount to shift by
     * @param defaultValue the default value
     */
    public ShiftExpression(Expression output, long by, Expression defaultValue) {
        super("$shift");
        this.output = output;
        this.by = by;
        this.defaultValue = defaultValue;
    }

    /**
     * @return the output
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression output() {
        return output;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the amount to shift by
     */
    @MorphiaInternal
    public long by() {
        return by;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the default values
     */
    @MorphiaInternal
    public Expression defaultValue() {
        return defaultValue;
    }
}

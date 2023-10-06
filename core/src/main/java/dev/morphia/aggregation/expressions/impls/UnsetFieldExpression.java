package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class UnsetFieldExpression extends Expression {
    private final Expression field;
    private final Object input;

    /**
     * @param field the field
     * @param input the input
     */
    public UnsetFieldExpression(Expression field, Object input) {
        super("$unsetField");
        this.field = field;
        this.input = input;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the field
     */
    @MorphiaInternal
    public Expression field() {
        return field;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the input
     */
    @MorphiaInternal
    public Object input() {
        return input;
    }
}

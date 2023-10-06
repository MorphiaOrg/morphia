package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class SetFieldExpression extends Expression {
    private final Expression field;
    private final Object input;
    private final Expression value;

    /**
     * @param field the field
     * @param input the input
     * @param value the value
     */
    public SetFieldExpression(Expression field, Object input, Expression value) {
        super("$setField");
        this.field = field;
        this.input = input;
        this.value = value;
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

    /**
     * @hidden
     * @morphia.internal
     * @return the value
     */
    @MorphiaInternal
    @Override
    public Expression value() {
        return value;
    }
}

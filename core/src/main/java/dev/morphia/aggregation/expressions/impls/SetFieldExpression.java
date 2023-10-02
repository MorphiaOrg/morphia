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

    public SetFieldExpression(Expression field, Object input, Expression value) {
        super("$setField");
        this.field = field;
        this.input = input;
        this.value = value;
    }

    public Expression field() {
        return field;
    }

    public Object input() {
        return input;
    }

    @Override
    public Expression value() {
        return value;
    }
}

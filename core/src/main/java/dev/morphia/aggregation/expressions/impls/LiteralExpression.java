package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class LiteralExpression extends Expression {
    public LiteralExpression(Object value) {
        super("$literal", new ValueExpression(value));
    }
}

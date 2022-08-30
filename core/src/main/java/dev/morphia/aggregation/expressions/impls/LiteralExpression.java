package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;

public class LiteralExpression extends Expression {
    public LiteralExpression(Object value) {
        super("$literal", new ValueExpression(value));
    }
}

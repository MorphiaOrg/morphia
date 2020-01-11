package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.aggregation.experimental.expressions.Expression.DocumentExpression;
import dev.morphia.sofia.Sofia;

public class ReplaceRoot extends Stage {
    private Expression value;
    private DocumentExpression document;

    protected ReplaceRoot() {
        super("$replaceRoot");
    }

    public static ReplaceRoot with() {
        return new ReplaceRoot();
    }

    public ReplaceRoot field(final String name, final Expression expression) {
        if (value != null) {
            throw new IllegalStateException(Sofia.mixedModesNotAllowed(getStageName()));
        }
        if (document == null) {
            document = Expression.of();
        }
        document.field(name, expression);

        return this;
    }

    public DocumentExpression getDocument() {
        return document;
    }

    public Expression getValue() {
        return value;
    }

    public ReplaceRoot value(final Expression expression) {
        if (document != null) {
            throw new IllegalStateException(Sofia.mixedModesNotAllowed(getStageName()));
        }
        value = expression;
        return this;
    }
}

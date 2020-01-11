package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.Expression;

public class Redact extends Stage {
    private Expression expression;
    protected Redact() {
        super("$redact");
    }

    public static Redact on(final Expression expression) {
        Redact redact = new Redact();
        redact.expression = expression;
        return redact;
    }

    public Expression getExpression() {
        return expression;
    }
}

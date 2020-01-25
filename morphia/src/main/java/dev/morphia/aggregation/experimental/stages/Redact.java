package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.impls.Expression;

/**
 * Restricts the contents of the documents based on information stored in the documents themselves.
 *
 * @mongodb.driver.manual reference/operator/aggregation/redact/ $redact
 */
public class Redact extends Stage {
    private Expression expression;

    protected Redact() {
        super("$redact");
    }

    /**
     * Creates a redaction stage with the given expression
     *
     * @param expression the expression
     * @return the new field
     */
    public static Redact on(final Expression expression) {
        Redact redact = new Redact();
        redact.expression = expression;
        return redact;
    }

    /**
     * @return the expression
     * @morphia.internal
     */
    public Expression getExpression() {
        return expression;
    }
}

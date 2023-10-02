package dev.morphia.aggregation.expressions;

import dev.morphia.aggregation.expressions.impls.Expression;

public class DocumentNumberExpression extends Expression {
    public DocumentNumberExpression() {
        super("$documentNumber");
    }
}

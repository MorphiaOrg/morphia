package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class DocumentNumberExpression extends Expression {
    public DocumentNumberExpression() {
        super("$documentNumber");
    }
}

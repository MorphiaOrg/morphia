package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @since 3.0
 */
public class MedianExpression extends Expression {
    /**
     * @param input the input expression
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public MedianExpression(Expression input) {
        super("$median", input);
    }
}

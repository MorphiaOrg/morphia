package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Defines the values to be merged.
 */
public class MergeObjects extends Expression {

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public MergeObjects() {
        super("$mergeObjects", new ExpressionList());
    }

    /**
     * Adds an expression to be merged
     *
     * @param expression the expression
     * @return this
     */
    @SuppressWarnings("unchecked")
    public MergeObjects add(Expression expression) {
        ExpressionList value = value();
        if (value != null) {
            value.add(expression);
        }
        return this;
    }

    @Override
    public ExpressionList value() {
        return (ExpressionList) super.value();
    }
}

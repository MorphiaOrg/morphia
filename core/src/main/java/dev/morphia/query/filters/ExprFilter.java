package dev.morphia.query.filters;

import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class ExprFilter extends Filter {
    /**
     * @param expression the expression
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ExprFilter(Expression expression) {
        super("$expr", null, expression);
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @Override
    @MorphiaInternal
    public Expression getValue() {
        return (Expression) super.getValue();
    }
}

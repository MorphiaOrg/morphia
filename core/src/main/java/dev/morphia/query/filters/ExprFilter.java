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
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ExprFilter(Expression expression) {
        super("$expr", null, expression);
    }

    @Override
    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression getValue() {
        return (Expression) super.getValue();
    }
}

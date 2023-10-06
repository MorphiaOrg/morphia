package dev.morphia.aggregation.stages;

import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Groups incoming documents based on the value of a specified expression, then computes the count of documents in each distinct group.
 *
 * @aggregation.expression $sortByCount
 */
public class SortByCount extends Stage {
    private final Expression expression;

    /**
     * @param expression the expression
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected SortByCount(Expression expression) {
        super("$sortByCount");
        this.expression = expression;
    }

    /**
     * Creates a new stage grouping by the given expression.
     *
     * @param expression the expression
     * @return this
     * @since 2.2
     */
    public static SortByCount sortByCount(Expression expression) {
        return new SortByCount(expression);
    }

    /**
     * @return the expression
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Expression expression() {
        return expression;
    }
}

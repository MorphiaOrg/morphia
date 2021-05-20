package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.impls.Expression;

/**
 * Groups incoming documents based on the value of a specified expression, then computes the count of documents in each distinct group.
 *
 * @aggregation.expression $sortByCount
 */
public class SortByCount extends Stage {
    private final Expression expression;

    protected SortByCount(Expression expression) {
        super("$sortByCount");
        this.expression = expression;
    }

    /**
     * Creates a new stage grouping by the given expression.
     *
     * @param expression the expression
     * @return this
     * @deprecated use {@link #sortByCount(Expression)}
     */
    @Deprecated(forRemoval = true)
    public static SortByCount on(Expression expression) {
        return new SortByCount(expression);
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
     * @morphia.internal
     */
    public Expression getExpression() {
        return expression;
    }
}

package dev.morphia.aggregation.stages;

import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.annotations.internal.MorphiaInternal;

import static dev.morphia.aggregation.expressions.Expressions.wrap;

/**
 * Groups incoming documents based on the value of a specified expression, then computes the count of documents in each distinct group.
 *
 * @aggregation.stage $sortByCount
 */
public class SortByCount extends Stage {
    private final Expression expression;

    /**
     * @param expression the expression
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected SortByCount(Object expression) {
        super("$sortByCount");
        this.expression = wrap(expression);
    }

    /**
     * Creates a new stage grouping by the given expression.
     *
     * @param expression the expression
     * @return this
     * @since 2.2
     * @aggregation.stage $sortByCount
     * @mongodb.server.release 3.4
     */
    public static SortByCount sortByCount(Object expression) {
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

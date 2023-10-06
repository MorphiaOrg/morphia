package dev.morphia.aggregation.expressions.impls;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.query.Sort;
import dev.morphia.sofia.Sofia;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class SortArrayExpression extends Expression {
    private final Expression input;

    private final Sort[] sort;

    /**
     * @param input the input
     * @param sort  the sort
     */
    public SortArrayExpression(Expression input, Sort... sort) {
        super("$sortArray");
        if (sort.length == 0) {
            throw new IllegalArgumentException(Sofia.atLeastOneSortRequired());
        }

        this.input = input;
        this.sort = sort;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the input
     */
    @MorphiaInternal
    public Expression input() {
        return input;
    }

    /**
     * @hidden
     * @morphia.internal
     * @return the sort
     */
    @MorphiaInternal
    public Sort[] sort() {
        return sort;
    }
}

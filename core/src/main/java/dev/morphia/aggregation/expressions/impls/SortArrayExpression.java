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

    public SortArrayExpression(Expression input, Sort... sort) {
        super("$sortArray");
        if (sort.length == 0) {
            throw new IllegalArgumentException(Sofia.atLeastOneSortRequired());
        }

        this.input = input;
        this.sort = sort;
    }

    public Expression input() {
        return input;
    }

    public Sort[] sort() {
        return sort;
    }
}

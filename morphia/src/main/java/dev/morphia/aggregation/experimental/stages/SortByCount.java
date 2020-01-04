package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.Expression;

public class SortByCount extends Stage {
    private Expression expression;

    protected SortByCount(final Expression expression) {
        super("$sortByCount");
        this.expression = expression;
    }

    public static SortByCount on(final Expression expression) {
        return new SortByCount(expression);
    }

    public Expression getExpression() {
        return expression;
    }
}

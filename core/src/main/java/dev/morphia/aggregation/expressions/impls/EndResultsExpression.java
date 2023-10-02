package dev.morphia.aggregation.expressions.impls;

/**
 * Gives first/last results
 */
public class EndResultsExpression extends Expression {
    private final Expression input;
    private final Expression n;

    public EndResultsExpression(String operation, Expression n, Expression input) {
        super(operation);
        this.input = input;
        this.n = n;
    }

    public Expression input() {
        return input;
    }

    public Expression n() {
        return n;
    }
}

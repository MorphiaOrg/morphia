package dev.morphia.aggregation.expressions.impls;

/**
 * @aggregation.expression $shift
 * @mongodb.server.release 5.0
 * @since 2.3
 */
public class ShiftExpression extends Expression {
    private final Expression output;
    private final long by;
    private final Expression defaultValue;

    public ShiftExpression(Expression output, long by, Expression defaultValue) {
        super("$shift");
        this.output = output;
        this.by = by;
        this.defaultValue = defaultValue;
    }

    public Expression output() {
        return output;
    }

    public long by() {
        return by;
    }

    public Expression defaultValue() {
        return defaultValue;
    }
}

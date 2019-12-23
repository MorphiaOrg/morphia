package dev.morphia.aggregation.experimental.stages;

public class Accumulator extends Expression {
    protected Accumulator(final String operation, final String name, final Object value) {
        super(operation, name, value);
    }

    public static Accumulator sum(final String name, final Object value) {
        return new Accumulator("$sum", name, value);
    }
}

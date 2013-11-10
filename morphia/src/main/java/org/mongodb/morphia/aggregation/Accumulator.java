package org.mongodb.morphia.aggregation;

public class Accumulator {
    private final String operation;
    private final String field;

    public Accumulator(final String operation, final String field) {
        this.operation = operation;
        this.field = field;
    }

    public String getField() {
        return field;
    }

    public String getOperation() {
        return operation;
    }
}

package org.mongodb.morphia.aggregation;

public class Accumulator {
    private final String operation;
    private final Object field;

    public Accumulator(final String operation, final String field) {
        this(operation, (Object) ("$" + field));
    }

    public Accumulator(final String operation, final Object field) {
        this.operation = operation;
        this.field = field;
    }

    public Object getField() {
        return field;
    }

    public String getOperation() {
        return operation;
    }
}

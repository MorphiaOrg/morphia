package dev.morphia.aggregation.experimental.stages;

public abstract class Expression {
    protected final String operation;
    protected final String name;
    protected final Object value;

    protected Expression(final String operation, final String name, final Object value) {
        this.operation = operation;
        this.name = name;
        this.value = value;
    }

    public String getOperation() {
        return operation;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }
}

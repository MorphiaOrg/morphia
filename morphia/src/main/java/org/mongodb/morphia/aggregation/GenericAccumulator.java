package org.mongodb.morphia.aggregation;

public class GenericAccumulator implements IAccumulator {

    private final String operation;
    private final Object value;

    public GenericAccumulator(String operation, Object value) {
        this.operation = operation;
        this.value = value;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String getOperation() {
        return operation;
    }

}

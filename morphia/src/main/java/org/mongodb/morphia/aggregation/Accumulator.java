package org.mongodb.morphia.aggregation;

/**
 * Use this class to accumulate value of the filed using the given operation
 *
 */
public class Accumulator implements IAccumulator {

    private final String operation;
    private final String field;

    public Accumulator(final String operation, final String field) {
        this.operation = operation;
        this.field = "$" + field;
    }

    @Override
    public String getValue() {
        return field;
    }

    @Override
    public String getOperation() {
        return operation;
    }
    
}

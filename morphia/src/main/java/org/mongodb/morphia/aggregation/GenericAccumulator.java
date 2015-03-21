package org.mongodb.morphia.aggregation;

/**
 * Use this class to accumulate generic values with given operation.
 * This class is convenient to count results using Integer 1 as the value.
 * 
 * @author Lukas Zaruba, lukas.zaruba@gmail.com
 *
 */
public class GenericAccumulator implements IAccumulator {

    private final String operation;
    private final Object value;

    public GenericAccumulator(final String operation, final Object value) {
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

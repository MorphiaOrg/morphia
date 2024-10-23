package dev.morphia.query.updates;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class BitOperator extends UpdateOperator {
    private final String operation;

    BitOperator(String operation, String field, int value) {
        super("$bit", field, value);
        this.operation = operation;
    }

    public String operation() {
        return operation;
    }

}

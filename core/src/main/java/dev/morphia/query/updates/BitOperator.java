package dev.morphia.query.updates;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.query.OperationTarget;

import org.bson.Document;

class BitOperator extends UpdateOperator {
    private final String operation;

    BitOperator(String operation, String field, int value) {
        super("$bit", field, value);
        this.operation = operation;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public OperationTarget toTarget(PathTarget pathTarget) {
        return new OperationTarget(pathTarget, new Document(operation, value()));
    }

}

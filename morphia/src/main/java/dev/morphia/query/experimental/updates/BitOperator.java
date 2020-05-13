package dev.morphia.query.experimental.updates;

import dev.morphia.internal.PathTarget;
import dev.morphia.query.OperationTarget;
import org.bson.Document;

class BitOperator extends UpdateOperator {
    private final String operation;

    public BitOperator(final String operation, final String field, final int value) {
        super("$bit", field, value);
        this.operation = operation;
    }

    @Override
    public OperationTarget toTarget(final PathTarget pathTarget) {
        return new OperationTarget(pathTarget, new Document(operation, value()));
    }

}

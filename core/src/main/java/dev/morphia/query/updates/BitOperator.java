package dev.morphia.query.updates;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.codec.pojo.EntityModel;
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
    public OperationTarget toOperationTarget(MorphiaDatastore datastore, EntityModel model, boolean validate) {
        var pathTarget = new PathTarget(datastore.getMapper(), model, field(), validate);
        return new OperationTarget(pathTarget, new Document(operation, value()));
    }

}

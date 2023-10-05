package dev.morphia.query.updates;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.query.OperationTarget;

import org.bson.Document;

/**
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
class UnsetOperator extends UpdateOperator {
    /**
     * @param field the field
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public UnsetOperator(String field) {
        super("$unset", field, "unused");
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public OperationTarget toOperationTarget(MorphiaDatastore datastore, EntityModel model, boolean validate) {
        var pathTarget = new PathTarget(datastore.getMapper(), model, field(), validate);

        return new OperationTarget(pathTarget, "") {
            @Override
            public Object encode(MorphiaDatastore datastore) {
                return new Document(pathTarget.translatedPath(), "");
            }
        };
    }
}

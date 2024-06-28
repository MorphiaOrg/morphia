package dev.morphia.query.updates;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.query.OperationTarget;

import org.bson.Document;

import static dev.morphia.mapping.codec.CodecHelper.coalesce;

/**
 * @hidden
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
public class UnsetOperator extends UpdateOperator {
    /**
     * @param field  the first field
     * @param others any other fields
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public UnsetOperator(String field, String[] others) {
        super("$unset", "", coalesce(field, others));
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

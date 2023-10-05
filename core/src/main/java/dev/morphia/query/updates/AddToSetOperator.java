package dev.morphia.query.updates;

import java.util.Collection;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.query.OperationTarget;

import org.bson.Document;

/**
 * Defines the $addToSet operator
 *
 * @since 2.0
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
class AddToSetOperator extends UpdateOperator {
    private final boolean each;

    /**
     * @param field  the field
     * @param values the values
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public AddToSetOperator(String field, Object values) {
        super("$addToSet", field, values);
        each = values instanceof Collection;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public OperationTarget toOperationTarget(MorphiaDatastore datastore, EntityModel model, boolean validate) {
        var pathTarget = new PathTarget(datastore.getMapper(), model, field(), validate);
        return new OperationTarget(pathTarget, each ? new Document("$each", value()) : value());
    }

}

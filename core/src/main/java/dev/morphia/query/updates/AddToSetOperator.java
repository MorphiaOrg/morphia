package dev.morphia.query.updates;

import java.util.Collection;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
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
    public OperationTarget toTarget(PathTarget pathTarget) {
        return new OperationTarget(pathTarget, each ? new Document("$each", value()) : value());
    }

}

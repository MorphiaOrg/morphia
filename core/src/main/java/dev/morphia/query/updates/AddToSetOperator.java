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
 */
public class AddToSetOperator extends UpdateOperator {
    private final boolean each;

    /**
     * @param field  the field
     * @param values the values
     * @morphia.internal
     */
    @MorphiaInternal
    public AddToSetOperator(String field, Object values) {
        super("$addToSet", field, values);
        each = values instanceof Collection;
    }

    @Override
    public OperationTarget toTarget(PathTarget pathTarget) {
        if (each) {
            return new OperationTarget(pathTarget, new Document("$each", value()));
        } else {
            return new OperationTarget(pathTarget, value());
        }
    }

}

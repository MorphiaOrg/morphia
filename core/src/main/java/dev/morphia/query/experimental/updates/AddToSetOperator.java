package dev.morphia.query.experimental.updates;

import dev.morphia.internal.PathTarget;
import dev.morphia.query.OperationTarget;
import org.bson.Document;

import java.util.Collection;

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

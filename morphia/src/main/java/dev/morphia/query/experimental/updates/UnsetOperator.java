package dev.morphia.query.experimental.updates;

import dev.morphia.internal.PathTarget;
import dev.morphia.query.OperationTarget;

/**
 * @morphia.internal
 * @since 2.0
 */
public class UnsetOperator extends UpdateOperator {
    /**
     * @param field the field
     * @morphia.internal
     */
    public UnsetOperator(final String field) {
        super("$unset", field, "unused");
    }

    @Override
    public OperationTarget toTarget(final PathTarget pathTarget) {
        return new OperationTarget(pathTarget, null);
    }
}

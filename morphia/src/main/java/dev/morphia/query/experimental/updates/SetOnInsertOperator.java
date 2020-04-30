package dev.morphia.query.experimental.updates;

import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.OperationTarget;

import java.util.Map;

/**
 * @morphia.internal
 * @since 2.0
 */
public class SetOnInsertOperator extends UpdateOperator {
    private Map<String, Object> insertValues;

    /**
     * @param values the values
     * @morphia.internal
     */
    public SetOnInsertOperator(final Map<String, Object> values) {
        super("$setOnInsert", "unused", "unused");
        insertValues = values;
    }

    @Override
    public OperationTarget toTarget(final PathTarget pathTarget) {
        return new OperationTarget(null, null) {
            @Override
            public Object encode(final Mapper mapper) {
                return insertValues;
            }
        };
    }
}

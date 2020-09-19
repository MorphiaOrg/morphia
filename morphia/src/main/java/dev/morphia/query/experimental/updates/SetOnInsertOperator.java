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
    private final Map<String, Object> insertValues;

    /**
     * @param values the values
     * @morphia.internal
     */
    public SetOnInsertOperator(Map<String, Object> values) {
        super("$setOnInsert", "unused", "unused");
        insertValues = values;
    }

    @Override
    public OperationTarget toTarget(PathTarget pathTarget) {
        return new OperationTarget(null, null) {
            @Override
            public Object encode(Mapper mapper) {
                return insertValues;
            }
        };
    }
}

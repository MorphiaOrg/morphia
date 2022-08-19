package dev.morphia.query.experimental.updates;

import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.query.OperationTarget;

import java.util.LinkedHashMap;
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
        Map<String, Object> mappedNames = new LinkedHashMap<>();
        Mapper mapper = pathTarget.mapper();
        EntityModel model = mapper.getEntityModel(pathTarget.root().getType());
        insertValues.forEach((key, value) -> {
            PathTarget keyTarget = new PathTarget(mapper, model, key, true);
            mappedNames.put(keyTarget.translatedPath(), value);
        });

        return new OperationTarget(null, null) {
            @Override
            public Object encode(Mapper mapper) {
                return mappedNames;
            }
        };
    }
}

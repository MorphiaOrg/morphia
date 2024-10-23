package dev.morphia.query.updates;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @morphia.internal
 * @hidden
 * @since 2.0
 */
@MorphiaInternal
public class SetOnInsertOperator extends UpdateOperator {
    private final Map<String, Object> insertValues;

    /**
     * @param values the values
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public SetOnInsertOperator(Map<String, Object> values) {
        super("$setOnInsert", "unused", "unused");
        insertValues = new LinkedHashMap<>(values);
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Map<String, Object> insertValues() {
        return insertValues;
    }

}

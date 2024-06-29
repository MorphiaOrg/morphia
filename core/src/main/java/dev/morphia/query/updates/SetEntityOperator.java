package dev.morphia.query.updates;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
public class SetEntityOperator extends UpdateOperator {
    /**
     * @param value the value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public SetEntityOperator(Object value) {
        super("$set", "", value);
    }

}

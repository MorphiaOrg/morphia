package dev.morphia.query.updates;

import dev.morphia.annotations.internal.MorphiaInternal;

import static dev.morphia.mapping.codec.CodecHelper.coalesce;

/**
 * @hidden
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
public class UnsetOperator extends UpdateOperator {
    /**
     * @param field  the first field
     * @param others any other fields
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public UnsetOperator(String field, String[] others) {
        super("$unset", "", coalesce(field, others));
    }

}

package dev.morphia.query.updates;

import java.util.Collection;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Defines the $addToSet operator
 *
 * @since 2.0
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class AddToSetOperator extends UpdateOperator {
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

}

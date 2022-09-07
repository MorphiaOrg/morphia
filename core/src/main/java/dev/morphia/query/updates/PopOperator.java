package dev.morphia.query.updates;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Defines the $pop update operator.
 *
 * @since 2.0
 */
public class PopOperator extends UpdateOperator {
    /**
     * @param field the field
     * @morphia.internal
     */
    @MorphiaInternal
    public PopOperator(String field) {
        super("$pop", field, 1);
    }

    /**
     * Remove the first element rather than the last.
     *
     * @return this
     */
    public PopOperator removeFirst() {
        value(-1);
        return this;
    }
}

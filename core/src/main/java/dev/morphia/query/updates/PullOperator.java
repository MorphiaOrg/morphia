package dev.morphia.query.updates;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.query.filters.Filter;

/**
 * Defines an operator for $pull
 *
 * @since 2.0
 * @morphia.internal
 * @hidden
 */
@MorphiaInternal
public class PullOperator extends UpdateOperator {
    /**
     * @param field  the field
     * @param filter the filter to apply
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public PullOperator(String field, Filter... filter) {
        super("$pull", field, filter);
    }

    /**
     * @param field the field
     * @param value the value to pull
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public PullOperator(String field, Object value) {
        super("$pull", field, value instanceof Filter ? new Filter[] { (Filter) value } : value);
    }

}

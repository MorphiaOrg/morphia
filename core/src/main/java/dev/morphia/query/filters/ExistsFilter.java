package dev.morphia.query.filters;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class ExistsFilter extends Filter {
    /**
     * @param field the field
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ExistsFilter(String field) {
        super("$exists", field, null);
    }
}

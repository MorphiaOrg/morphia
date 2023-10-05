package dev.morphia.query.filters;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class EqFilter extends Filter {
    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public EqFilter(String field, Object val) {
        super("$eq", field, val);
    }
}

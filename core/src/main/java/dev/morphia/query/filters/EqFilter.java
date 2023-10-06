package dev.morphia.query.filters;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class EqFilter extends Filter {
    /**
     * @param field the field to check
     * @param val   the value to compare against
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public EqFilter(String field, @Nullable Object val) {
        super("$eq", field, val);
    }
}

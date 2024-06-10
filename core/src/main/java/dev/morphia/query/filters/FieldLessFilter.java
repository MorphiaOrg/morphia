package dev.morphia.query.filters;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class FieldLessFilter extends Filter {

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public FieldLessFilter(String name, Object val) {
        super(name, null, val);
    }

}

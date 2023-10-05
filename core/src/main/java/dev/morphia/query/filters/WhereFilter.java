package dev.morphia.query.filters;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class WhereFilter extends Filter {
    public WhereFilter(String val) {
        super("$where", null, val);
    }
}

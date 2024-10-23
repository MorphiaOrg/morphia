package dev.morphia.query.filters;

import java.util.List;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class ElemMatchFilter extends Filter {
    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    ElemMatchFilter(List<Filter> query) {
        super("$elemMatch", null, query);
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    ElemMatchFilter(String field, List<Filter> query) {
        super("$elemMatch", field, query);
    }
}

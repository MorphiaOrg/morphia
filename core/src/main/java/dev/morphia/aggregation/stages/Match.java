package dev.morphia.aggregation.stages;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.query.filters.Filter;

/**
 * Limits the number of documents passed to the next stage in the pipeline.
 *
 * @aggregation.expression $match
 */
public class Match extends Stage {
    private final Filter[] filters;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected Match(Filter... filters) {
        super("$match");
        this.filters = filters;
    }

    /**
     * Creates the new stage using the filters for matching
     *
     * @param filters the filters to apply
     * @return this
     * @since 2.2
     */
    public static Match match(Filter... filters) {
        return new Match(filters);
    }

    /**
     * @return the filters
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Filter[] getFilters() {
        return filters;
    }
}

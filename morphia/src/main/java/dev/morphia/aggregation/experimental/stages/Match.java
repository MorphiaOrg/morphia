package dev.morphia.aggregation.experimental.stages;

import dev.morphia.query.experimental.filters.Filter;

/**
 * Limits the number of documents passed to the next stage in the pipeline.
 *
 * @aggregation.expression $match
 */
public class Match extends Stage {
    private Filter[] filters;

    protected Match(final Filter... filters) {
        super("$match");
        this.filters = filters;
    }

    /**
     * Creates the new stage using the filters for matching
     *
     * @param filters the filters to apply
     * @return this
     */
    public static Match on(final Filter... filters) {
        return new Match(filters);
    }

    /**
     * @return the filters
     * @morphia.internal
     */
    public Filter[] getFilters() {
        return filters;
    }
}

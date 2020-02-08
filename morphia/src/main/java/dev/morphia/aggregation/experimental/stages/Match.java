package dev.morphia.aggregation.experimental.stages;

import dev.morphia.query.Query;

/**
 * Limits the number of documents passed to the next stage in the pipeline.
 *
 * @aggregation.expression $match
 */
public class Match extends Stage {
    private Query query;

    protected Match(final Query query) {
        super("$match");
        this.query = query;
    }

    /**
     * Creates the new stage using the query for matching
     *
     * @param query the query
     * @return this
     */
    public static Match on(final Query<?> query) {
        return new Match(query);
    }

    /**
     * @return the query
     * @morphia.internal
     */
    public Query getQuery() {
        return query;
    }
}

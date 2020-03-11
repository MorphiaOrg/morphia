package dev.morphia.aggregation.experimental.stages;

/**
 * Limits the number of documents passed to the next stage in the pipeline.
 *
 * @aggregation.expression $limit
 */
public class Limit extends Stage {
    private long limit;

    protected Limit(final long limit) {
        super("$limit");
        this.limit = limit;
    }

    /**
     * Creates the new stage.
     *
     * @param limit the limit to apply
     * @return this
     */
    public static Limit of(final long limit) {
        return new Limit(limit);
    }

    /**
     * @return the limit
     * @morphia.internal
     */
    public long getLimit() {
        return limit;
    }
}

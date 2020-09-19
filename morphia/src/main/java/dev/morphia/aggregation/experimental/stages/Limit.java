package dev.morphia.aggregation.experimental.stages;

/**
 * Limits the number of documents passed to the next stage in the pipeline.
 *
 * @aggregation.expression $limit
 */
public class Limit extends Stage {
    private final long limit;

    protected Limit(long limit) {
        super("$limit");
        this.limit = limit;
    }

    /**
     * Creates the new stage.
     *
     * @param limit the limit to apply
     * @return this
     */
    public static Limit of(long limit) {
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

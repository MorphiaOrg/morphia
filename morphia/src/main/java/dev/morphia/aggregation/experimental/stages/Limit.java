package dev.morphia.aggregation.experimental.stages;

/**
 * Limits the number of documents passed to the next stage in the pipeline.
 *
 * @mongodb.driver.manual reference/operator/aggregation/limit/ $limit
 */
public class Limit extends Stage {
    private int limit;

    protected Limit(final int limit) {
        super("$limit");
        this.limit = limit;
    }

    /**
     * Creates the new stage.
     *
     * @param limit the limit to apply
     * @return this
     */
    public static Limit of(final int limit) {
        return new Limit(limit);
    }

    /**
     * @return the limit
     * @morphia.internal
     */
    public int getLimit() {
        return limit;
    }
}

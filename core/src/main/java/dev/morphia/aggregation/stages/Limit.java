package dev.morphia.aggregation.stages;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Limits the number of documents passed to the next stage in the pipeline.
 *
 * @aggregation.stage $limit
 */
public class Limit extends Stage {
    private final long limit;

    /**
     * @param limit the limit to impose
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected Limit(long limit) {
        super("$limit");
        this.limit = limit;
    }

    /**
     * Creates the new stage.
     *
     * @param limit the limit to apply
     * @return this
     * @since 2.2
     * @aggregation.stage $limit
     */
    public static Limit limit(long limit) {
        return new Limit(limit);
    }

    /**
     * @return the limit
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public long limit() {
        return limit;
    }
}

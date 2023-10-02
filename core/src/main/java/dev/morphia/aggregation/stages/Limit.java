package dev.morphia.aggregation.stages;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Limits the number of documents passed to the next stage in the pipeline.
 *
 * @aggregation.expression $limit
 */
public class Limit extends Stage {
    private final long limit;

    /**
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
     */
    public static Limit limit(long limit) {
        return new Limit(limit);
    }

    /**
     * Creates the new stage.
     *
     * @param limit the limit to apply
     * @return this
     * @deprecated use {@link #limit(long)}
     */
    @Deprecated(forRemoval = true)
    public static Limit of(long limit) {
        return new Limit(limit);
    }

    /**
     * @return the limit
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public long getLimit() {
        return limit;
    }
}

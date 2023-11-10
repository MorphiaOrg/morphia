package dev.morphia.aggregation.stages;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Returns plan cache information for a collection. The stage returns a document for each plan cache entry.
 *
 * @aggregation.stage $planCacheStats
 */
public class PlanCacheStats extends Stage {
    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected PlanCacheStats() {
        super("$planCacheStats");
    }

    /**
     * Creates the new stage
     *
     * @return the new stage
     * @since 2.2
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public static PlanCacheStats planCacheStats() {
        return new PlanCacheStats();
    }
}

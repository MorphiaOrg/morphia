package dev.morphia.aggregation.experimental.stages;

/**
 * Returns plan cache information for a collection. The stage returns a document for each plan cache entry.
 *
 * @aggregation.expression $planCacheStats
 */
public class PlanCacheStats extends Stage {
    protected PlanCacheStats() {
        super("$planCacheStats");
    }

    /**
     * Creates the new stage
     *
     * @return the new stage
     * @deprecated use {@link #planCacheStats()}
     */
    @Deprecated(forRemoval = true)
    public static PlanCacheStats of() {
        return new PlanCacheStats();
    }

    /**
     * Creates the new stage
     *
     * @return the new stage
     * @since 2.2
     */
    public static PlanCacheStats planCacheStats() {
        return new PlanCacheStats();
    }
}

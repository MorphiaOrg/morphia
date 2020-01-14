package dev.morphia.aggregation.experimental.stages;

/**
 * Returns plan cache information for a collection. The stage returns a document for each plan cache entry.
 *
 * @mongodb.driver.manual reference/operator/aggregation/planCacheStats/ $planCacheStats
 */
public class PlanCacheStats extends Stage {
    protected PlanCacheStats() {
        super("$planCacheStats");
    }

    /**
     * Creates the new stage
     *
     * @return the new stage
     */
    public static PlanCacheStats of() {
        return new PlanCacheStats();
    }
}

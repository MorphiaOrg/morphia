package dev.morphia.aggregation.experimental.stages;

public class PlanCacheStats extends Stage {
    protected PlanCacheStats() {
        super("$planCacheStats");
    }

    public static PlanCacheStats of() {
        return new PlanCacheStats();
    }
}

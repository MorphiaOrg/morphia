package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.PlanCacheStats.planCacheStats;
import static dev.morphia.query.filters.Filters.eq;

public class TestPlanCacheStats extends AggregationTest {
    public TestPlanCacheStats() {
        skipDataCheck();
    }

    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                planCacheStats()));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                planCacheStats(),
                match(eq("planCacheKey", "B1435201"))));
    }
}

package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.CurrentOp.currentOp;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.exists;

public class TestCurrentOp extends AggregationTest {
    public TestCurrentOp() {
        skipDataCheck();

    }

    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                currentOp()
                        .allUsers(true)
                        .idleSessions(true),
                match(eq("active", false), exists("transaction"))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                currentOp()
                        .allUsers(true)
                        .localOps(true),
                match(eq("desc", "query analyzer"))));
    }
}

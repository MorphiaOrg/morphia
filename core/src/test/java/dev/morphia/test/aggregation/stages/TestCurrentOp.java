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

    /**
     * test data: dev/morphia/test/aggregation/stages/currentOp/example1
     */
    @Test(testName = "Inactive Sessions  :: Replica Set")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(currentOp().allUsers(true).idleSessions(true),
                        match(eq("active", false), exists("transaction"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/currentOp/example2
     */
    @Test(testName = "Inactive Sessions  :: Sharded Cluster (localOps: true)")
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(currentOp().allUsers(true).idleSessions(true),
                        match(eq("active", false), exists("transaction"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/currentOp/example3
     */
    @Test(testName = "Inactive Sessions  :: Sharded Cluster")
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(currentOp().allUsers(true).idleSessions(true),
                        match(eq("active", false), exists("transaction"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/currentOp/example4
     */
    @Test(testName = "Sampled Queries :: Replica Set")
    public void testExample4() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation
                .pipeline(currentOp().allUsers(true).localOps(true), match(eq("desc", "query analyzer"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/currentOp/example5
     */
    @Test(testName = "Sampled Queries :: Sharded Cluster: mongos")
    public void testExample5() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation
                .pipeline(currentOp().allUsers(true).localOps(true), match(eq("desc", "query analyzer"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/currentOp/example6
     */
    @Test(testName = "Sampled Queries :: Sharded Cluster: mongod --shardsvr")
    public void testExample6() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation
                .pipeline(currentOp().allUsers(true).localOps(true), match(eq("desc", "query analyzer"))));
    }
}

package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.stages.CurrentOp.currentOp;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.exists;

public class TestCurrentOp extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/currentOp/inactiveSessionsReplicaSet
     */
    @Test
    @DisplayName("Inactive Sessions  :: Replica Set")
    public void testInactiveSessionsReplicaSet() {
        testPipeline(new ActionTestOptions().skipDataCheck(true),
                (aggregation) -> aggregation.pipeline(currentOp().allUsers(true).idleSessions(true),
                        match(eq("active", false), exists("transaction"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/currentOp/inactiveSessionsShardedClusterLocalOpsTrue
     */
    @Test
    @DisplayName("Inactive Sessions  :: Sharded Cluster (localOps: true)")
    public void testInactiveSessionsShardedClusterLocalOpsTrue() {
        testPipeline(new ActionTestOptions().skipDataCheck(true),
                (aggregation) -> aggregation.pipeline(currentOp().allUsers(true).idleSessions(true),
                        match(eq("active", false), exists("transaction"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/currentOp/inactiveSessionsShardedCluster
     */
    @Test
    @DisplayName("Inactive Sessions  :: Sharded Cluster")
    public void testInactiveSessionsShardedCluster() {
        testPipeline(new ActionTestOptions().skipDataCheck(true),
                (aggregation) -> aggregation.pipeline(currentOp().allUsers(true).idleSessions(true),
                        match(eq("active", false), exists("transaction"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/currentOp/sampledQueriesReplicaSet
     */
    @Test
    @DisplayName("Sampled Queries :: Replica Set")
    public void testSampledQueriesReplicaSet() {
        testPipeline(new ActionTestOptions().skipDataCheck(true), (aggregation) -> aggregation
                .pipeline(currentOp().allUsers(true).localOps(true), match(eq("desc", "query analyzer"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/currentOp/sampledQueriesShardedClusterMongos
     */
    @Test
    @DisplayName("Sampled Queries :: Sharded Cluster: mongos")
    public void testSampledQueriesShardedClusterMongos() {
        testPipeline(new ActionTestOptions().skipDataCheck(true), (aggregation) -> aggregation
                .pipeline(currentOp().allUsers(true).localOps(true), match(eq("desc", "query analyzer"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/currentOp/sampledQueriesShardedClusterMongodShardsvr
     */
    @Test
    @DisplayName("Sampled Queries :: Sharded Cluster: mongod --shardsvr")
    public void testSampledQueriesShardedClusterMongodShardsvr() {
        testPipeline(new ActionTestOptions().skipDataCheck(true), (aggregation) -> aggregation
                .pipeline(currentOp().allUsers(true).localOps(true), match(eq("desc", "query analyzer"))));
    }
}

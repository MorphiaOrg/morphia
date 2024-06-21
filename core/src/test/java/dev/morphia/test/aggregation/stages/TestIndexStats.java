package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.IndexStats.indexStats;

public class TestIndexStats extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/indexStats/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true)
                .skipDataCheck(true), (aggregation) -> aggregation.pipeline(indexStats()));
    }
}

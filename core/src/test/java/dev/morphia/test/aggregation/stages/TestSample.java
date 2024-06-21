package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.Sample.sample;

public class TestSample extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/sample/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true)
                .skipDataCheck(true), (aggregation) -> aggregation.pipeline(sample(3)));
    }
}

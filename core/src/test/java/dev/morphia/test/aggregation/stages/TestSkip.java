package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.Skip.skip;

public class TestSkip extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/skip/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().skipDataCheck(true), (aggregation) -> aggregation.pipeline(skip(5)));
    }
}

package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.Unset.unset;

public class TestUnset extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/unset/example1
     * 
     */
    @Test(testName = "Remove a Single Field")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(unset("copies")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/unset/example2
     * 
     */
    @Test(testName = "Remove Top-Level Fields")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation.pipeline(unset("isbn", "copies")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/unset/example3
     * 
     */
    @Test(testName = "Remove Embedded Fields")
    public void testExample3() {
        testPipeline((aggregation) -> aggregation.pipeline(unset("isbn", "author.first", "copies.warehouse")));
    }
}

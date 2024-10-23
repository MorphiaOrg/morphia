package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.range;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestRange extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/range/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().suppressId().include("city").include("Rest stops", range(0, "$distance").step(25))));
    }

}

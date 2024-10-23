package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.rtrim;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestRtrim extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/rtrim/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("item").include("description", rtrim("$description"))));
    }

}

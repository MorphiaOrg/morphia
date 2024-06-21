package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.isoDayOfWeek;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestIsoDayOfWeek extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/isoDayOfWeek/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().suppressId().include("name", "$name").include("dayOfWeek", isoDayOfWeek("$birthday"))));
    }

}

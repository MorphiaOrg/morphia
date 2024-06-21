package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.isoWeek;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestIsoWeek extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/isoWeek/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().suppressId().include("city", "$city").include("weekNumber", isoWeek("$date"))));
    }

}

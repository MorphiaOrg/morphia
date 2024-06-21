package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.isoWeekYear;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestIsoWeekYear extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/isoWeekYear/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("yearNumber", isoWeekYear("$date"))));
    }

}

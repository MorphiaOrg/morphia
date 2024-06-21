package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.BooleanExpressions.not;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestNot extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/not/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("item").include("result", not(gt("$qty", 250)))));
    }

}

package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.ceil;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestCeil extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/ceil/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("value").include("ceilingValue", ceil("$value"))));
    }

}

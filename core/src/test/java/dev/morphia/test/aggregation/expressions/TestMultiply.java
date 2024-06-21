package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestMultiply extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/multiply/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("date").include("item").include("total", multiply("$price", "$quantity"))));
    }

}

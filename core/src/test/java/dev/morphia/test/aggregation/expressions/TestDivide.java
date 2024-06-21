package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.divide;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestDivide extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/divide/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("city").include("workdays", divide("$hours", 8))));
    }

}

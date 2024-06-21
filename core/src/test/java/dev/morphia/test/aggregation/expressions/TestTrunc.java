package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.trunc;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestTrunc extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/trunc/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("truncatedValue", trunc("$value", 1))));
    }

}

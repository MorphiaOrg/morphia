package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.pow;
import static dev.morphia.aggregation.expressions.WindowExpressions.stdDevPop;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestPow extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/pow/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().include("variance", pow(stdDevPop("$scores.score"), 2))));
    }

}

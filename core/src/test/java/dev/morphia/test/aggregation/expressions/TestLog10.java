package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.log10;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestLog10 extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/log10/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("pH", multiply(-1, log10("$H3O")))));
    }

}

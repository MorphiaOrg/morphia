package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.ln;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestLn extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/ln/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("x", "$year").include("y", ln("$sales"))));
    }

}

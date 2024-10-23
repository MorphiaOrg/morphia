package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.expressions.MathExpressions.pow;
import static dev.morphia.aggregation.expressions.MathExpressions.sqrt;
import static dev.morphia.aggregation.expressions.MathExpressions.subtract;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSqrt extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/sqrt/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("distance",
                sqrt(add(pow(subtract("$p2.y", "$p1.y"), 2), pow(subtract("$p2.x", "$p1.x"), 2))))));
    }

}

package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.expressions.MathExpressions.floor;
import static dev.morphia.aggregation.expressions.MathExpressions.log;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestLog extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/log/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        Expression value;
        testPipeline(
                (aggregation) -> aggregation.pipeline(project().include("bitsNeeded", floor(add(1, log("$int", 2))))));
    }

}

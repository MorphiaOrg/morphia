package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.ln;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestLn extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/ln/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(project().include("x", "$year").include("y", ln("$sales"))));
    }

}

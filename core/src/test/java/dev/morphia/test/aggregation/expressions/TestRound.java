package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.round;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestRound extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/round/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(project().include("roundedValue", round("$value", 1))));
    }

}

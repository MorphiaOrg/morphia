package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.pow;
import static dev.morphia.aggregation.expressions.WindowExpressions.stdDevPop;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestPow extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("variance", pow(stdDevPop("$scores.score"), 2))));
    }

}

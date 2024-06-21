package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.expressions.BooleanExpressions.and;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.gte;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.lt;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.switchExpression;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSwitch extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/switch/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(project().include("name")
                .include("summary", switchExpression().branch(gte(avg("$scores"), 90), "Doing great!")
                        .branch(and(gte(avg("$scores"), 80), lt(avg("$scores"), 90)), "Doing pretty well.")
                        .branch(lt(avg("$scores"), 80), "Needs improvement.").defaultCase("No scores found."))));
    }

}

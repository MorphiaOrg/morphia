package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.gte;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.test.ServerVersion.ANY;

public class TestCond extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/cond/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(ANY, false, true, aggregation -> aggregation
                .pipeline(project().include("item").include("discount", condition(gte("$qty", 250), 30, 20))));

    }
}

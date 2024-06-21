package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.lte;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestLte extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/lte/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project().suppressId().include("item").include("qty").include("qtyLte250", lte("$qty", 250))));
    }

}

package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.reverseArray;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestReverseArray extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/reverseArray/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation
                .pipeline(project().include("name").include("reverseFavorites", reverseArray("$favorites"))));
    }

}

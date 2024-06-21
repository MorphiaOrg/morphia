package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.isoDayOfWeek;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestIsoDayOfWeek extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/isoDayOfWeek/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project().suppressId().include("name", "$name").include("dayOfWeek", isoDayOfWeek("$birthday"))));
    }

}

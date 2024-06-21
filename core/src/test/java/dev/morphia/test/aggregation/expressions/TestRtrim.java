package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.rtrim;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestRtrim extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/rtrim/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation
                .pipeline(project().include("item").include("description", rtrim("$description"))));
    }

}

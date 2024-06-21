package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.trim;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestTrim extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/trim/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation
                .pipeline(project().include("item").include("description", trim("$description"))));
    }

}

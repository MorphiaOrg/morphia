package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.indexOfCP;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestIndexOfCP extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/indexOfCP/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, false,
                (aggregation) -> aggregation.pipeline(project().include("cpLocation", indexOfCP("$item", "foo"))));
    }

}

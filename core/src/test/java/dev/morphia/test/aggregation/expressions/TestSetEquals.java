package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.SetExpressions.setEquals;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSetEquals extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/setEquals/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(project().suppressId()
                .include("cakes").include("cupcakes").include("sameFlavors", setEquals("$cakes", "$cupcakes"))));
    }

}

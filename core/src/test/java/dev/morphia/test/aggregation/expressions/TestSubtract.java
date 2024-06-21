package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.expressions.MathExpressions.subtract;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSubtract extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/subtract/example1
     * 
     */
    @Test(testName = "Subtract Numbers")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation
                .pipeline(project().include("item").include("total", subtract(add("$price", "$fee"), "$discount"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/subtract/example2
     * 
     */
    @Test(testName = "Subtract Two Dates")
    public void testExample2() {
        skipDataCheck();
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation
                .pipeline(project().include("item").include("dateDifference", subtract("$$NOW", "$date"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/subtract/example3
     * 
     */
    @Test(testName = "Subtract Milliseconds from a Date")
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation
                .pipeline(project().include("item").include("dateDifference", subtract("$date", 5 * 60 * 1000))));
    }
}

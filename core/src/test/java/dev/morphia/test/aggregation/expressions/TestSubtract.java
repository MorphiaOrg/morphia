package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.expressions.MathExpressions.subtract;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSubtract extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/subtract/example1
     * 
     */
    @Test(testName = "Subtract Numbers")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation.pipeline(
                        project().include("item").include("total", subtract(add("$price", "$fee"), "$discount"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/subtract/example2
     * 
     */
    @Test(testName = "Subtract Two Dates")
    public void testExample2() {
        testPipeline(
                new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true)
                        .skipDataCheck(true),
                (aggregation) -> aggregation
                        .pipeline(project().include("item").include("dateDifference", subtract("$$NOW", "$date"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/subtract/example3
     * 
     */
    @Test(testName = "Subtract Milliseconds from a Date")
    public void testExample3() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation.pipeline(
                        project().include("item").include("dateDifference", subtract("$date", 5 * 60 * 1000))));
    }
}

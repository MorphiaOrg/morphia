package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.test.ServerVersion.ANY;

public class TestAdd extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/add/example1
     * 
     */
    @Test(testName = "Add Numbers")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation
                        .pipeline(project().include("item").include("total", add("$price", "$fee"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/add/example2
     * 
     */
    @Test(testName = "Perform Addition on a Date")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion(ANY).removeIds(false).orderMatters(true),
                aggregation -> aggregation
                        .pipeline(project().include("item", 1).include("billing_date", add("$date", 259200000))));

    }
}

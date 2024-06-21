package dev.morphia.test.aggregation.stages;

import dev.morphia.aggregation.expressions.TimeUnit;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;

public class TestSetWindowFields extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/setWindowFields/example1
     * 
     */
    @Test(testName = "Documents Window Examples")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(false),
                (aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                        .sortBy(ascending("orderDate")).output(output("cumulativeQuantityForState")
                                .operator(sum("$quantity")).window().documents("unbounded", "current"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/setWindowFields/example2
     * 
     */
    @Test(testName = "Range Window Example")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state").sortBy(ascending("price"))
                        .output(output("quantityFromSimilarOrders").operator(sum("$quantity")).window().range(-10,
                                10))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/setWindowFields/example3
     * 
     */
    @Test(testName = "Time Range Window Examples")
    public void testExample3() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                        .sortBy(ascending("orderDate")).output(output("recentOrders").operator(push("$orderDate"))
                                .window().range("unbounded", 10, TimeUnit.MONTH))));
    }

}

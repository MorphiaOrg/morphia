package dev.morphia.test.aggregation.stages;

import dev.morphia.aggregation.expressions.TimeUnit;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;

public class TestSetWindowFields extends AggregationTest {
    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                setWindowFields()
                        .partitionBy("$state")
                        .sortBy(ascending("orderDate"))
                        .output(output("cumulativeQuantityForState")
                                .operator(sum("$quantity"))
                                .window()
                                .documents("unbounded", "current"))));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                setWindowFields()
                        .partitionBy("$state")
                        .sortBy(ascending("price"))
                        .output(output("quantityFromSimilarOrders")
                                .operator(sum("$quantity"))
                                .window()
                                .range(-10, 10))));
    }

    @Test
    public void testExample4() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                setWindowFields()
                        .partitionBy("$state")
                        .sortBy(ascending("orderDate"))
                        .output(output("recentOrders")
                                .operator(push("$orderDate"))
                                .window()
                                .range("unbounded", 10, TimeUnit.MONTH))));
    }

}

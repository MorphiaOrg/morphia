package dev.morphia.test.aggregation.stages;

import dev.morphia.aggregation.expressions.TimeUnit;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.WindowExpressions.shift;
import static dev.morphia.aggregation.stages.Set.set;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;

public class TestSetWindowFields extends TemplatedTestBase {
    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.0.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/setWindowFields/documentsWindowExamples
     * 
     */
    @Test
    @DisplayName("Documents Window Examples")
    public void testDocumentsWindowExamples() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                        .sortBy(ascending("orderDate")).output(output("cumulativeQuantityForState")
                                .operator(sum("$quantity")).window().documents("unbounded", "current"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/setWindowFields/rangeWindowExample
     * 
     */
    @Test
    @DisplayName("Range Window Example")
    public void testRangeWindowExample() {
        testPipeline((aggregation) -> aggregation
                .pipeline(setWindowFields().partitionBy("$state").sortBy(ascending("price")).output(
                        output("quantityFromSimilarOrders").operator(sum("$quantity")).window().range(-10, 10))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/setWindowFields/timeRangeWindowExamples
     * 
     */
    @Test
    @DisplayName("Time Range Window Examples")
    public void testTimeRangeWindowExamples() {
        testPipeline((aggregation) -> aggregation.pipeline(
                setWindowFields().partitionBy("$state").sortBy(ascending("orderDate")).output(output("recentOrders")
                        .operator(push("$orderDate")).window().range("unbounded", 10, TimeUnit.MONTH))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/setWindowFields/comparisonWithPreviousValuesExample
     * 
     * db.cakeSales.aggregate( [ { $setWindowFields: { partitionBy: "$type", sortBy:
     * { orderDate: 1 }, output: { previousPrice: { $shift: { output: "$price", by:
     * -1 } } } } }, { $set: { priceComparison: { $cond: [ { $eq: ["$price",
     * "$previousPrice"] }, "same", { $cond: [ { $gt: ["$price", "$previousPrice"]
     * }, "higher", "lower" ] } ] } } }, ] )
     */
    @Test
    @DisplayName("Comparison with Previous Values Example")
    public void testComparisonWithPreviousValuesExample() {
        testPipeline((aggregation) -> aggregation.pipeline(
                setWindowFields().partitionBy("$type").sortBy(ascending("orderDate"))
                        .output(output("previousPrice").operator(shift("$price", -1))),
                set().field("priceComparison", condition(eq("$price", "$previousPrice"), "same",
                        condition(gt("$price", "$previousPrice"), "higher", "lower")))));
    }

}

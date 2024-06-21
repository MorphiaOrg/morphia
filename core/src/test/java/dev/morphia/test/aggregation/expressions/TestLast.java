package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.last;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.*;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.Sort.*;

public class TestLast extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/last/example1
     * 
     */
    @Test(testName = "Use in ``$group`` Stage")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, false,
                (aggregation) -> aggregation.pipeline(sort().ascending("item").ascending("date"),
                        group(id("$item")).field("lastSalesDate", last("$date"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/last/example2
     * 
     */
    @Test(testName = "Use in ``$setWindowFields`` Stage")
    public void testExample2() {
        // this example starts importing a bunch of stuff and gets more complicated than
        // I care
        // to make the parser support. we have working examples already.
        /*
         * testPipeline(ServerVersion.ANY, false, true, (aggregation) ->
         * aggregation.pipeline( setWindowFields() .partitionBy("$state")
         * .sortBy(ascending("orderDate")) .output(output("lastOrderTypeForState")
         * .operator(last("$type")) .window() .documents("current", "unbounded"))));
         */
    }

}

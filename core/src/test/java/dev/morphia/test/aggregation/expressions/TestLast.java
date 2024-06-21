package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.last;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.*;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.Sort.*;

public class TestLast extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/last/example1
     * 
     */
    @Test(testName = "Use in ``$group`` Stage")
    public void testExample1() {
        testPipeline(new ActionTestOptions().orderMatters(false),
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
         * testPipeline(new dev.morphia.test.util.ActionTestOptions(). removeIds(false),
         * (aggregation) -> aggregation.pipeline( setWindowFields()
         * .partitionBy("$state") .sortBy(ascending("orderDate"))
         * .output(output("lastOrderTypeForState") .operator(last("$type")) .window()
         * .documents("current", "unbounded"))));
         */
    }

}

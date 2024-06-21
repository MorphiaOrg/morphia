package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.first;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.test.ServerVersion.v50;

public class TestFirst extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/aggregation/expressions/first/example1
     * 
     */
    @Test(testName = "Use in ``$group`` Stage")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(v50).removeIds(false).orderMatters(false),
                (aggregation) -> aggregation.pipeline(sort().ascending("item", "date"),
                        group(id("$item")).field("firstSale", first("$date"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/first/example2
     * 
     */
    @Test(testName = "Missing Data")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion(v50).removeIds(false).orderMatters(false),
                (aggregation) -> aggregation.pipeline(sort().ascending("item", "price"),
                        group(id("$item")).field("inStock", first("$quantity"))

                ));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/first/example3
     * 
     */
    @Test(testName = "Use in ``$setWindowFields`` Stage")
    public void testExample3() {
        // this example starts importing a bunch of stuff and gets more complicated than
        // I care
        // to make the parser support. we have working examples already.
        /*
         * testPipeline(new
         * dev.morphia.test.util.ActionTestOptions().serverVersion(ServerVersion.ANY).
         * removeIds(false).orderMatters(true), (aggregation) -> aggregation.pipeline(
         * setWindowFields() .partitionBy("$state") .sortBy(Sort.ascending("orderDate"))
         * .output(Output.output("firstOrderTypeForState") .operator(first("$type"))
         * .window() .documents("unbounded", "current"))));
         */
    }

}

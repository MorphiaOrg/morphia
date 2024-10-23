package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.WindowExpressions.rank;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.Sort.descending;

public class TestRank extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/rank/example1
     * 
     */
    @Test(testName = "Rank Partitions by an Integer Field")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                .sortBy(descending("quantity")).output(output("rankQuantityForState").operator(rank()))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/rank/example2
     * 
     */
    @Test(testName = "Rank Partitions by a Date Field")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                .sortBy(ascending("orderDate")).output(output("rankOrderDateForState").operator(rank()))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/rank/example3
     * 
     */
    @Test(testName = "Rank Partitions Containing Duplicate Values, Nulls, or Missing Data")
    public void testExample3() {
        testPipeline((aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                .sortBy(descending("quantity")).output(output("rankQuantityForState").operator(rank()))));
    }

}

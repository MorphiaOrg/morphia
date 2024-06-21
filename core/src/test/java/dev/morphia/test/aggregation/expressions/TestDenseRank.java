package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.WindowExpressions.denseRank;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.*;

public class TestDenseRank extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/denseRank/example1
     * 
     */
    @Test(testName = "Dense Rank Partitions by an Integer Field")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation
                        .pipeline(setWindowFields().partitionBy("$state").sortBy(descending("quantity"))
                                .output(output("denseRankQuantityForState").operator(denseRank()))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/denseRank/example2
     * 
     */
    @Test(testName = "Dense Rank Partitions by a Date Field")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation
                        .pipeline(setWindowFields().partitionBy("$state").sortBy(ascending("orderDate"))
                                .output(output("denseRankOrderDateForState").operator(denseRank()))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/denseRank/example3
     * 
     */
    @Test(testName = "Dense Rank for Duplicate, Null, and Missing Values")
    public void testExample3() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation
                        .pipeline(setWindowFields().partitionBy("$state").sortBy(descending("quantity"))
                                .output(output("denseRankQuantityForState").operator(denseRank()))));
    }

}

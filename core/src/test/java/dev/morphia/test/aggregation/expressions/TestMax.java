package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.max;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.ascending;

public class TestMax extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/max/example1
     * 
     */
    @Test(testName = "Use in ``$group`` Stage")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(group(id("$item"))
                .field("maxTotalAmount", max(multiply("$price", "$quantity"))).field("maxQuantity", max("$quantity"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/max/example2
     * 
     */
    @Test(testName = "Use in ``$project`` Stage")
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(project().include("quizMax", max("$quizzes"))
                        .include("labMax", max("$labs")).include("examMax", max("$final", "$midterm"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/max/example3
     * 
     */
    @Test(testName = "Use in ``$setWindowFields`` Stage")
    public void testExample3() {
        testPipeline(ServerVersion.v50, false, true,
                (aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                        .sortBy(ascending("orderDate")).output(output("maximumQuantityForState")
                                .operator(max("$quantity")).window().documents("unbounded", "current"))));
    }

}

package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.WindowExpressions.stdDevPop;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.*;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.query.Sort.*;

public class TestStdDevPop extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/stdDevPop/example1
     * 
     */
    @Test(testName = "Use in ``$group`` Stage")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, false,
                (aggregation) -> aggregation.pipeline(group(id("$quiz")).field("stdDev", stdDevPop("$score"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/stdDevPop/example2
     * 
     */
    @Test(testName = "Use in ``$project`` Stage")
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(project().include("stdDev", stdDevPop("$scores.score"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/stdDevPop/example3
     * 
     */
    @Test(testName = "Use in ``$setWindowFields`` Stage")
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, false,
                (aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                        .sortBy(ascending("orderDate")).output(output("stdDevPopQuantityForState")
                                .operator(stdDevPop("$quantity")).window().documents("unbounded", "current"))

                ));
    }

}

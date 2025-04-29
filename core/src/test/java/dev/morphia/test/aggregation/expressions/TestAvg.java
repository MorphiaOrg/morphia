package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.stages.Projection;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;

public class TestAvg extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/avg/example1
     * 
     */
    @Test(testName = "Use in ``$group`` Stage")
    public void testExample1() {
        testPipeline(new ActionTestOptions().orderMatters(false), aggregation -> aggregation.pipeline(group(id("$item"))
                .field("avgAmount", avg(multiply("$price", "$quantity"))).field("avgQuantity", avg("$quantity"))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/avg/example2
     * 
     */
    @Test(testName = "Use in ``$project`` Stage")
    public void testExample2() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                aggregation -> aggregation.project(Projection.project().include("quizAvg", avg("$quizzes"))
                        .include("labAvg", avg("$labs")).include("examAvg", avg("$final", "$midterm"))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/avg/example3
     * 
     */
    @Test(testName = "Use in ``$setWindowFields`` Stage")
    public void testExample3() {
        // this has an include and throws off the parser
        /*
         * testPipeline(new dev.morphia.test.util.ActionTestOptions()
         * .orderMatters(false), aggregation -> aggregation
         * .setWindowFields(SetWindowFields.setWindowFields() .partitionBy("$state")
         * .sortBy(ascending("orderDate")) .output(output("averageQuantityForState")
         * .operator(avg("$quantity")) .window() .documents("unbounded", "current"))
         * 
         * )
         * 
         * );
         */

    }
}

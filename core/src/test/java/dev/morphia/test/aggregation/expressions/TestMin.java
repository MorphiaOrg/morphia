package dev.morphia.test.aggregation.expressions;

import dev.morphia.query.Sort;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.min;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.test.ServerVersion.v50;

public class TestMin extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/min/example1
     * 
     */
    @Test(testName = "Use in ``$group`` Stage")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(v50).removeIds(false).orderMatters(false),
                (aggregation) -> aggregation.pipeline(group(id("$item")).field("minQuantity", min("$quantity"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/min/example2
     * 
     */
    @Test(testName = "Use in ``$project`` Stage")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion(v50).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation.pipeline(project().include("quizMin", min("$quizzes"))
                        .include("labMin", min("$labs")).include("examMin", min("$final", "$midterm"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/min/example3
     * 
     */
    @Test(testName = "Use in ``$setWindowFields`` Stage")
    public void testExample3() {
        testPipeline(new ActionTestOptions().serverVersion(v50).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                        .sortBy(Sort.ascending("orderDate")).output(output("minimumQuantityForState")
                                .operator(min("$quantity")).window().documents("unbounded", "current"))));
    }

}

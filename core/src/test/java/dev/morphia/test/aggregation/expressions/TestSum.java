package dev.morphia.test.aggregation.expressions;

import dev.morphia.query.Sort;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.DateExpressions.dayOfYear;
import static dev.morphia.aggregation.expressions.DateExpressions.year;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.output;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.test.ServerVersion.v50;

public class TestSum extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/sum/example1
     * 
     */
    @Test(testName = "Use in ``$group`` Stage")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(v50).orderMatters(false),
                (aggregation) -> aggregation
                        .pipeline(group(id(document().field("day", dayOfYear("$date")).field("year", year("$date"))))
                                .field("totalAmount", sum(multiply("$price", "$quantity"))).field("count", sum(1))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/sum/example2
     * 
     */
    @Test(testName = "Use in ``$project`` Stage")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("quizTotal", sum("$quizzes"))
                .include("labTotal", sum("$labs")).include("examTotal", sum("$final", "$midterm"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/sum/example3
     * 
     */
    @Test(testName = "Use in ``$setWindowFields`` Stage")
    public void testExample3() {
        testPipeline((aggregation) -> aggregation.pipeline(setWindowFields().partitionBy("$state")
                .sortBy(Sort.ascending("orderDate")).output(output("sumQuantityForState").operator(sum("$quantity"))
                        .window().documents("unbounded", "current"))));
    }

}

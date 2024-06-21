package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.concatArrays;
import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Set.set;
import static dev.morphia.query.filters.Filters.eq;

public class TestSet extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/set/example1
     * 
     */
    @Test(testName = "Using Two ``$set`` Stages")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(
                set().field("totalHomework", sum("$homework")).field("totalQuiz", sum("$quiz")),
                set().field("totalScore", add("$totalHomework", "$totalQuiz", "$extraCredit"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/set/example2
     * 
     */
    @Test(testName = "Adding Fields to an Embedded Document")
    public void testExample2() {
        testPipeline((aggregation) -> aggregation.pipeline(set().field("specs.fuel_type", "unleaded")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/set/example3
     * 
     */
    @Test(testName = "Overwriting an existing field")
    public void testExample3() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation.pipeline(set().field("cats", 20)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/set/example4
     * 
     */
    @Test(testName = "Add Element to an Array")
    public void testExample4() {
        testPipeline((aggregation) -> aggregation.pipeline(match(eq("_id", 1)),
                set().field("homework", concatArrays("$homework", array(7)))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/set/example5
     * 
     */
    @Test(testName = "Creating a New Field with Existing Fields")
    public void testExample5() {
        testPipeline((aggregation) -> aggregation.pipeline(set().field("quizAverage", avg("$quiz"))));
    }

}

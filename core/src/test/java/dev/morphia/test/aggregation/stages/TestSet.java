package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.concatArrays;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Set.set;
import static dev.morphia.query.filters.Filters.eq;

public class TestSet extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                set()
                        .field("totalHomework", sum(field("homework")))
                        .field("totalQuiz", sum(field("quiz"))),
                set()
                        .field("totalScore", add(field("totalHomework"),
                                field("totalQuiz"), field("extraCredit")))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                set()
                        .field("specs.fuel_type", value("unleaded"))));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                set()
                        .field("cats", value(20))));
    }

    @Test
    public void testExample4() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                match(eq("_id", 1)),
                set()
                        .field("homework", concatArrays(field("homework"), array(value(7))))));
    }

    @Test
    public void testExample5() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                set()
                        .field("quizAverage", avg(field("quiz")))));
    }

}

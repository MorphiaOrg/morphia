package dev.morphia.test.aggregation.stages;

import dev.morphia.aggregation.expressions.StringExpressions;
import dev.morphia.aggregation.stages.Fill.Method;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ConditionalExpressions.ifNull;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.TypeExpressions.toBool;
import static dev.morphia.aggregation.stages.Fill.fill;
import static dev.morphia.aggregation.stages.Set.set;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.test.DriverVersion.v42;
import static dev.morphia.test.ServerVersion.v53;

public class TestFill extends AggregationTest {
    @Test
    public void testExample1() {
        minDriver = v42;
        testPipeline(v53, aggregation -> aggregation.pipeline(
                fill()
                        .field("bootsSold", 0)
                        .field("sandalsSold", 0)
                        .field("sneakersSold", 0)));
    }

    @Test
    public void testExample2() {
        testPipeline(v53, aggregation -> aggregation.pipeline(
                fill()
                        .sortBy(ascending("time"))
                        .field("price", Method.LINEAR)));
    }

    @Test
    public void testExample3() {
        minDriver = v42;

        testPipeline(v53, aggregation -> aggregation.pipeline(
                fill()
                        .sortBy(ascending("date"))
                        .field("score", Method.LOCF)));
    }

    @Test
    public void testExample4() {
        minDriver = v42;
        testPipeline(v53, aggregation -> aggregation.pipeline(
                fill()
                        .sortBy(ascending("date"))
                        .partitionBy(document("restaurant", "$restaurant"))
                        .field("score", Method.LOCF)));
    }

    @Test
    public void testExample5() {
        testPipeline(v53, true, true, (aggregation) -> aggregation.pipeline(
                set()
                        .field("valueExisted", ifNull()
                                .target(toBool(StringExpressions.toString("$score")))
                                .replacement(false)),
                fill()
                        .sortBy(ascending("date"))
                        .field("score", Method.LOCF)));
    }

}

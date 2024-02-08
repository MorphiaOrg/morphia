package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.test.ServerVersion.ANY;

public class TestAdd extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("item")
                        .include("total", add("$price", "$fee"))));
    }

    @Test
    public void testExample2() {
        testPipeline(ANY, false, true, aggregation -> aggregation
                .pipeline(project()
                        .include("item", 1)
                        .include("billing_date",
                                add("$date", 259200000))));

    }
}

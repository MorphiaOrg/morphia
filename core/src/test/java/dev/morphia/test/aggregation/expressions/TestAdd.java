package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.test.ServerVersion.ANY;

public class TestAdd extends AggregationTest {
    @Test
    public void testExample2() {
        testPipeline(ANY, false, true, aggregation -> aggregation
                .project(project()
                        .include("item", value(1))
                        .include("total",
                                add(field("price"), field("fee")))));

    }

    @Test
    public void testExample3() {
        testPipeline(ANY, false, true, aggregation -> aggregation
                .project(project()
                        //        { $project: { item: 1, billing_date: { $add: [ "$date", 3*24*60*60000 ] } } }

                        .include("item", value(1))
                        .include("billing_date",
                                add(field("date"), value(259200000)))));

    }
}

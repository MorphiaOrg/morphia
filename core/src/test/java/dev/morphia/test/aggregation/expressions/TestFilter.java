package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.gte;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.filter;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestFilter extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("items", filter(field("items"),
                                gte(value("$$item.price"), value(100)))
                                .as("item"))

        ));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("items", filter(field("items"),
                                gte(value("$$item.price"), value(100)))
                                .as("item")
                                .limit(value(1)))

        ));
    }

    @Test
    public void testExample3() {
        // this example is API incompatible with morphia since limit can only be an int and not a floating point number
    }

    @Test
    public void testExample4() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("items", filter(field("items"),
                                gte(value("$$item.price"), value(100)))
                                .as("item")
                                .limit(value(5)))

        ));
    }

}

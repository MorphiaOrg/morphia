package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.MathExpressions.abs;
import static dev.morphia.aggregation.expressions.MathExpressions.subtract;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.test.ServerVersion.ANY;

public class TestAbs extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ANY, false, true, aggregation -> aggregation.pipeline(
                project()
                        .include("delta",
                                abs(subtract(field("startTemp"), field("endTemp"))))));

    }
}

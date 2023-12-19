package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.MathExpressions.add;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.expressions.VariableExpressions.let;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestLet extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("finalTotal", let(multiply(value("$$total"), value("$$discounted")))
                                .variable("total", add(field("price"), field("tax")))
                                .variable("discounted",
                                        condition(field("applyDiscount"), value(0.9), value(1))))));
    }

}

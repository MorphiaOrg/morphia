package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.DriverVersion;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.expressions.ComparisonExpressions.lte;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.literal;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.Miscellaneous.getField;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.expr;

public class TestGetField extends AggregationTest {
    @Test
    public void testExample1() {
        minDriver = DriverVersion.v43;
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                match(expr(gt(getField("price.usd"), value(200))))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                match(expr(gt(getField(literal("$price")), value(200))))));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                match(
                        expr(
                                lte(
                                        getField(literal("$small"))
                                                .input(field("quantity")),
                                        value(20))))));
    }

}

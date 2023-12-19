package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.StringExpressions;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.switchExpression;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.expressions.StringExpressions.concat;
import static dev.morphia.aggregation.expressions.TypeExpressions.convert;
import static dev.morphia.aggregation.expressions.TypeExpressions.type;
import static dev.morphia.aggregation.expressions.impls.ConvertType.DECIMAL;
import static dev.morphia.aggregation.expressions.impls.ConvertType.INT;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestConvert extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                addFields()
                        .field("convertedPrice", convert(
                                field("price"), DECIMAL)
                                .onError(value("Error"))
                                .onNull(value(0.0)))
                        .field("convertedQty", convert(field("qty"), INT)
                                .onError(concat(
                                        value("Could not convert "),
                                        StringExpressions.toString(field("qty")),
                                        value(" to type integer.")))
                                .onNull(value(0))),
                project()
                        .include("totalPrice", switchExpression()
                                .branch(eq(
                                        type(field("convertedPrice")),
                                        value("string")), value("NaN"))
                                .branch(eq(
                                        type(field("convertedQty")),
                                        value("string")), value("NaN"))
                                .defaultCase(multiply(field("convertedPrice"), field("convertedQty"))))));
    }

}

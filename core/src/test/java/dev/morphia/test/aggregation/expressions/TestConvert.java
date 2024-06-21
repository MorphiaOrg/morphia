package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.StringExpressions;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.switchExpression;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.expressions.StringExpressions.concat;
import static dev.morphia.aggregation.expressions.TypeExpressions.convert;
import static dev.morphia.aggregation.expressions.TypeExpressions.type;
import static dev.morphia.aggregation.expressions.impls.ConvertType.DECIMAL;
import static dev.morphia.aggregation.expressions.impls.ConvertType.INT;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestConvert extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/convert/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                addFields().field("convertedPrice", convert("$price", DECIMAL).onError("Error").onNull(0.0))
                        .field("convertedQty",
                                convert("$qty", INT).onError(concat("Could not convert ",
                                        StringExpressions.toString("$qty"), " to type integer.")).onNull(0)),
                project().include("totalPrice",
                        switchExpression().branch(eq(type("$convertedPrice"), "string"), "NaN")
                                .branch(eq(type("$convertedQty"), "string"), "NaN")
                                .defaultCase(multiply("$convertedPrice", "$convertedQty")))));
    }

}

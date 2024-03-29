package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.expressions.TypeExpressions.toDecimal;
import static dev.morphia.aggregation.expressions.TypeExpressions.toInt;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestToDecimal extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                addFields()
                        .field("convertedPrice", toDecimal("$price"))
                        .field("convertedQty", toInt("$qty")),
                project()
                        .include("item")
                        .include("totalPrice",
                                multiply("$convertedPrice", "$convertedQty"))));
    }

}

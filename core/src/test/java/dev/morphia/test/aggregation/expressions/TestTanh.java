package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.degreesToRadians;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.tanh;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestTanh extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, true, true, (aggregation) -> aggregation.pipeline(
                addFields()
                        .field("tanh_output", tanh(degreesToRadians(field("angle"))))));
    }

}

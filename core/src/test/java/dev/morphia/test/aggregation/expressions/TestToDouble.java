package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.expressions.StringExpressions.substrBytes;
import static dev.morphia.aggregation.expressions.TypeExpressions.toDouble;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestToDouble extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                addFields()
                        .field("degrees", toDouble(substrBytes(field("temp"), value(0), value(4))))));
    }

}

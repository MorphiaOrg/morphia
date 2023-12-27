package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.TrigonometryExpressions.acosh;
import static dev.morphia.aggregation.expressions.TrigonometryExpressions.radiansToDegrees;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.test.ServerVersion.ANY;

public class TestAcosh extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ANY, false, true, aggregation -> aggregation
                .pipeline(addFields()
                        .field("y-coordinate",
                                radiansToDegrees(acosh("$x-coordinate")))));

    }
}

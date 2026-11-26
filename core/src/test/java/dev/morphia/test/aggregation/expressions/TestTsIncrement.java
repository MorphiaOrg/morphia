package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.tsIncrement;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestTsIncrement extends TemplatedTestBase {

    @Test
    @DisplayName("Obtain the Incrementing Ordinal from a Timestamp Field")
    public void testObtainTheIncrementingOrdinalFromATimestampField() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().suppressId().include("saleTimestamp").include("saleIncrement", tsIncrement("$saleTimestamp"))));
    }
}

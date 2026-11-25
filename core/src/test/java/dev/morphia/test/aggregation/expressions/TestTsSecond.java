package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.tsSecond;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestTsSecond extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.1.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/tsSecond/obtainTheNumberOfSecondsFromATimestampField
     * 
     */
    @Test
    @DisplayName("Obtain the Number of Seconds from a Timestamp Field")
    public void testObtainTheNumberOfSecondsFromATimestampField() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().suppressId().include("saleTimestamp").include("saleSeconds", tsSecond("$saleTimestamp"))));
    }
}

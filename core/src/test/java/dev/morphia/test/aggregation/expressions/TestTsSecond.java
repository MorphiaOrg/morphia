package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

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
     * test data: dev/morphia/test/aggregation/expressions/tsSecond/example1
     * 
     */
    @Test
    @DisplayName("Obtain the Number of Seconds from a Timestamp Field")
    public void testExample1() {
        testPipeline(new ActionTestOptions(), (aggregation) -> aggregation.pipeline(
                project().suppressId().include("saleTimestamp").include("saleSeconds", tsSecond("$saleTimestamp"))));
    }
}

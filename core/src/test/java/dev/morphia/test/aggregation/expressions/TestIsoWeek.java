package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.isoWeek;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestIsoWeek extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/isoWeek/isoWeekExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testIsoWeekExpressionOperator() {
        testPipeline((aggregation) -> aggregation
                .pipeline(project().suppressId().include("city", "$city").include("weekNumber", isoWeek("$date"))));
    }

}

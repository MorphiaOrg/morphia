package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.isoWeekYear;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestIsoWeekYear extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/isoWeekYear/isoWeekYearExpressionOperator
     * 
     */
    @Test
    @DisplayName("main")
    public void testIsoWeekYearExpressionOperator() {
        testPipeline((aggregation) -> aggregation.pipeline(project().include("yearNumber", isoWeekYear("$date"))));
    }

}

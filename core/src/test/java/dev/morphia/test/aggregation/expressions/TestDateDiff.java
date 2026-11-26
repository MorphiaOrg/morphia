package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.TimeUnit;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.expressions.DateExpressions.dateDiff;
import static dev.morphia.aggregation.expressions.MathExpressions.trunc;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Projection.project;
import static java.time.DayOfWeek.*;

public class TestDateDiff extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.0.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/dateDiff/elapsedTime
     * 
     */
    @Test
    @DisplayName("Elapsed Time")
    public void testElapsedTime() {
        testPipeline((aggregation) -> aggregation.pipeline(
                group(id(null)).field("averageTime", avg(dateDiff("$purchased", "$delivered", TimeUnit.DAY))),
                project().suppressId().include("numDays", trunc("$averageTime", 1))

        ));
    }

}

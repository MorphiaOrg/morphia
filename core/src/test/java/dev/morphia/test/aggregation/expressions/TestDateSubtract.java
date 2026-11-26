package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.TimeUnit;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.dateSubtract;
import static dev.morphia.aggregation.expressions.DateExpressions.dateToString;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestDateSubtract extends TemplatedTestBase {

    @Test
    @DisplayName("Adjust for Daylight Savings Time")
    public void testAdjustForDaylightSavingsTime() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().suppressId().include("location").include("start", dateToString().format("%Y-%m-%d %H:%M").date("$login"))
                        .include("days",
                                dateToString().format("%Y-%m-%d %H:%M").date(dateSubtract("$login", 1, TimeUnit.DAY).timezone("$location")))
                        .include("hours",
                                dateToString().format("%Y-%m-%d %H:%M")
                                        .date(dateSubtract("$login", 24, TimeUnit.HOUR).timezone("$location")))
                        .include("startTZInfo", dateToString().format("%Y-%m-%d %H:%M").date("$login").timeZone("$location"))
                        .include("daysTZInfo",
                                dateToString().format("%Y-%m-%d %H:%M").date(dateSubtract("$login", 1, TimeUnit.DAY).timezone("$location"))
                                        .timeZone("$location"))
                        .include("hoursTZInfo", dateToString().format("%Y-%m-%d %H:%M")
                                .date(dateSubtract("$login", 24, TimeUnit.HOUR).timezone("$location")).timeZone("$location"))));
    }
}

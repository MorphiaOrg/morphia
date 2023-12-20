package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.dateToString;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestDateToString extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.v70, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("yearMonthDayUTC", dateToString().date(field("date")).format("%Y-%m-%d"))
                        .include("timewithOffsetNY",
                                dateToString().date(field("date")).format("%H:%M:%S:%L%z").timeZone("America/New_York"))
                        .include("timewithOffset430", dateToString().date(field("date")).format("%H:%M:%S:%L%z").timeZone("+04:30"))
                        .include("minutesOffsetNY", dateToString().date(field("date")).format("%Z").timeZone("America/New_York"))
                        .include("minutesOffset430", dateToString().date(field("date")).format("%Z").timeZone("+04:30"))
                        .include("abbreviated_month", dateToString().date(field("date")).format("%b").timeZone("+04:30"))
                        .include("full_month", dateToString().date(field("date")).format("%B").timeZone("+04:30"))));
    }

}

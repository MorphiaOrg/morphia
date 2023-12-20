package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.dayOfMonth;
import static dev.morphia.aggregation.expressions.DateExpressions.dayOfWeek;
import static dev.morphia.aggregation.expressions.DateExpressions.dayOfYear;
import static dev.morphia.aggregation.expressions.DateExpressions.hour;
import static dev.morphia.aggregation.expressions.DateExpressions.milliseconds;
import static dev.morphia.aggregation.expressions.DateExpressions.minute;
import static dev.morphia.aggregation.expressions.DateExpressions.month;
import static dev.morphia.aggregation.expressions.DateExpressions.second;
import static dev.morphia.aggregation.expressions.DateExpressions.week;
import static dev.morphia.aggregation.expressions.DateExpressions.year;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestMonth extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("year", year(field("date")))
                        .include("month", month(field("date")))
                        .include("day", dayOfMonth(field("date")))
                        .include("hour", hour(field("date")))
                        .include("minutes", minute(field("date")))
                        .include("seconds", second(field("date")))
                        .include("milliseconds", milliseconds(field("date")))
                        .include("dayOfYear", dayOfYear(field("date")))
                        .include("dayOfWeek", dayOfWeek(field("date")))
                        .include("week", week(field("date")))));
    }

}

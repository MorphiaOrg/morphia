package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

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
import static dev.morphia.aggregation.stages.Projection.project;

public class TestYear extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/year/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation
                        .pipeline(project().include("year", year("$date")).include("month", month("$date"))
                                .include("day", dayOfMonth("$date")).include("hour", hour("$date"))
                                .include("minutes", minute("$date")).include("seconds", second("$date"))
                                .include("milliseconds", milliseconds("$date")).include("dayOfYear", dayOfYear("$date"))
                                .include("dayOfWeek", dayOfWeek("$date")).include("week", week("$date"))));
    }

}

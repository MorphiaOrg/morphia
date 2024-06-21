package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.dateToString;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestDateToString extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/dateToString/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.v70).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation.pipeline(project()
                        .include("yearMonthDayUTC", dateToString().date("$date").format("%Y-%m-%d"))
                        .include("timewithOffsetNY",
                                dateToString().date("$date").format("%H:%M:%S:%L%z").timeZone("America/New_York"))
                        .include("timewithOffset430",
                                dateToString().date("$date").format("%H:%M:%S:%L%z").timeZone("+04:30"))
                        .include("minutesOffsetNY",
                                dateToString().date("$date").format("%Z").timeZone("America/New_York"))
                        .include("minutesOffset430", dateToString().date("$date").format("%Z").timeZone("+04:30"))
                        .include("abbreviated_month", dateToString().date("$date").format("%b").timeZone("+04:30"))
                        .include("full_month", dateToString().date("$date").format("%B").timeZone("+04:30"))));
    }

}

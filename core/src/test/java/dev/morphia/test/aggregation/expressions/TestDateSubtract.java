package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.DateExpressions.dateSubtract;
import static dev.morphia.aggregation.expressions.DateExpressions.dateToString;
import static dev.morphia.aggregation.expressions.DateExpressions.month;
import static dev.morphia.aggregation.expressions.DateExpressions.year;
import static dev.morphia.aggregation.expressions.TimeUnit.DAY;
import static dev.morphia.aggregation.expressions.TimeUnit.HOUR;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.query.filters.Filters.expr;
import static dev.morphia.test.DriverVersion.v42;
import static dev.morphia.test.ServerVersion.v50;

public class TestDateSubtract extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/dateSubtract/example1
     * 
     */
    @Test(testName = "Subtract A Fixed Amount")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(v50).removeIds(true).orderMatters(false).minDriver(v42),
                (aggregation) -> aggregation.pipeline(
                        match(expr(eq(year("$logout"), 2021)), expr(eq(month("$logout"), 1))),
                        project().include("logoutTime", dateSubtract("$logout", 3, HOUR))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/dateSubtract/example2
     * 
     */
    @Test(testName = "Filter by Relative Dates")
    public void testExample2() {
        // $$NOW is a little pointless
        /*
         * testPipeline(new dev.morphia.test.util.ActionTestOptions().serverVersion(v50)
         * , (aggregation) -> { var epochTime = LocalDate.of(2021, Month.FEBRUARY, 22)
         * .toEpochDay();
         * 
         * return aggregation.pipeline( match(expr(gt(field("logoutTime"),
         * dateSubtract(value(epochTime), 1, WEEK)))), project() .suppressId()
         * .include("custId") .include("loggedOut", dateToString() .format("%Y-%m-%d")
         * .date(field("logoutTime")))); });
         */
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/dateSubtract/example3
     * 
     */
    @Test(testName = "Adjust for Daylight Savings Time")
    public void testExample3() {
        testPipeline(new ActionTestOptions().minDriver(v42), (aggregation) -> aggregation.pipeline(project()
                .suppressId().include("location")
                .include("start", dateToString().date("$login").format("%Y-%m-%d %H:%M"))
                .include("days",
                        dateToString().date(dateSubtract("$login", 1, DAY).timezone("$location"))
                                .format("%Y-%m-%d %H:%M"))
                .include("hours",
                        dateToString().date(dateSubtract("$login", 24, HOUR).timezone("$location"))
                                .format("%Y-%m-%d %H:%M"))
                .include("startTZInfo", dateToString().date("$login").format("%Y-%m-%d %H:%M").timeZone("$location"))
                .include("daysTZInfo",
                        dateToString().date(dateSubtract("$login", 1, DAY).timezone("$location"))
                                .format("%Y-%m-%d %H:%M").timeZone("$location"))
                .include("hoursTZInfo", dateToString().date(dateSubtract("$login", 24, HOUR).timezone("$location"))
                        .format("%Y-%m-%d %H:%M").timeZone("$location"))));
    }

}

package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.TimeUnit;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.expressions.DateExpressions.dateDiff;
import static dev.morphia.aggregation.expressions.MathExpressions.trunc;
import static dev.morphia.aggregation.expressions.TimeUnit.DAY;
import static dev.morphia.aggregation.expressions.TimeUnit.MONTH;
import static dev.morphia.aggregation.expressions.TimeUnit.WEEK;
import static dev.morphia.aggregation.expressions.TimeUnit.YEAR;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Projection.project;
import static java.time.DayOfWeek.*;
import static java.time.DayOfWeek.FRIDAY;

public class TestDateDiff extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/dateDiff/example1
     * 
     */
    @Test(testName = "Elapsed Time")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(
                        group(id(null)).field("averageTime", avg(dateDiff("$purchased", "$delivered", TimeUnit.DAY))),
                        project().suppressId().include("numDays", trunc("$averageTime", 1))

                ));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/dateDiff/example2
     * 
     */
    @Test(testName = "Result Precision")
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(project().suppressId().include("start", "$start")
                        .include("end", "$end").include("years", dateDiff("$start", "$end", YEAR))
                        .include("months", dateDiff("$start", "$end", MONTH))
                        .include("days", dateDiff("$start", "$end", DAY))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/dateDiff/example3
     * 
     */
    @Test(testName = "Weeks Per Month")
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation
                        .pipeline(project().suppressId().include("wks_default", dateDiff("$start", "$end", WEEK))
                                .include("wks_monday", dateDiff("$start", "$end", WEEK).startOfWeek(MONDAY))
                                .include("wks_friday", dateDiff("$start", "$end", WEEK).startOfWeek(FRIDAY))));
    }

}

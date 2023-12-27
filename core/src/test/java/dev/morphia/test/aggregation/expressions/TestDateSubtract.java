package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.DriverVersion;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.dateSubtract;
import static dev.morphia.aggregation.expressions.DateExpressions.dateToString;
import static dev.morphia.aggregation.expressions.TimeUnit.DAY;
import static dev.morphia.aggregation.expressions.TimeUnit.HOUR;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.test.ServerVersion.v50;

public class TestDateSubtract extends AggregationTest {
    public TestDateSubtract() {
        minDriver = DriverVersion.v42;
    }

    @Test
    public void testExample1() {
        testPipeline(v50, true, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("logoutTime",
                                dateSubtract("$logout", 3, HOUR))));
    }

    @Test
    public void testExample2() {
        // $$NOW is a little pointless
        /*
         * testPipeline(v50, false, true, (aggregation) -> {
         * var epochTime = LocalDate.of(2021, Month.FEBRUARY, 22)
         * .toEpochDay();
         * 
         * return aggregation.pipeline(
         * match(expr(gt(field("logoutTime"),
         * dateSubtract(value(epochTime), 1, WEEK)))),
         * project()
         * .suppressId()
         * .include("custId")
         * .include("loggedOut", dateToString()
         * .format("%Y-%m-%d")
         * .date(field("logoutTime"))));
         * });
         */
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .suppressId()
                        .include("location")
                        .include("start", dateToString()
                                .date("$login")
                                .format("%Y-%m-%d %H:%M"))
                        .include("days", dateToString()
                                .date(dateSubtract("$login", 1, DAY)
                                        .timezone("$location"))
                                .format("%Y-%m-%d %H:%M"))
                        .include("hours", dateToString()
                                .date(dateSubtract("$login", 24, HOUR)
                                        .timezone("$location"))
                                .format("%Y-%m-%d %H:%M"))
                        .include("startTZInfo", dateToString()
                                .date("$login")
                                .format("%Y-%m-%d %H:%M")
                                .timeZone("$location"))
                        .include("daysTZInfo", dateToString()
                                .date(dateSubtract("$login", 1, DAY)
                                        .timezone("$location"))
                                .format("%Y-%m-%d %H:%M")
                                .timeZone("$location"))
                        .include("hoursTZInfo", dateToString()
                                .date(dateSubtract("$login", 24, HOUR)
                                        .timezone("$location"))
                                .format("%Y-%m-%d %H:%M")
                                .timeZone("$location"))));
    }

}

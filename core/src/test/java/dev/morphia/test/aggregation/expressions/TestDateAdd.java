package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.TimeUnit;
import dev.morphia.test.DriverVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.expressions.DateExpressions.dateAdd;
import static dev.morphia.aggregation.expressions.DateExpressions.dateToString;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.query.filters.Filters.expr;
import static dev.morphia.test.ServerVersion.ANY;

public class TestDateAdd extends AggregationTest {
    @Test
    public void testExample1() {
        minDriver = DriverVersion.v42;
        testPipeline(ANY, true, true, aggregation -> aggregation.pipeline(
                project()
                        .include("expectedDeliveryDate",
                                dateAdd(field("purchaseDate"), 3, TimeUnit.DAY))));

    }

    @Test
    public void testExample2() {
        testPipeline(ANY, false, true, aggregation -> aggregation.pipeline(
                match(expr(
                        gt(field("deliveryDate"), dateAdd(field("purchaseDate"), 5, TimeUnit.DAY)))),

                project()
                        .suppressId()
                        .include("custId")
                        .include("purchased", dateToString()
                                .date(field("purchaseDate"))
                                .format("%Y-%m-%d"))
                        .include("delivery", dateToString()
                                .date(field("deliveryDate"))
                                .format("%Y-%m-%d"))

        ));

    }

    @Test
    public void testExample3() {
        testPipeline(ANY, false, true, aggregation -> aggregation.pipeline(
                project()
                        .suppressId()
                        .include("location")
                        .include("start", dateToString()
                                .date(field("login"))
                                .format("%Y-%m-%d %H:%M"))
                        .include("days", dateToString()
                                .date(dateAdd(field("login"), 1, TimeUnit.DAY)
                                        .timezone(field("location")))
                                .format("%Y-%m-%d %H:%M"))
                        .include("hours", dateToString()
                                .date(dateAdd(field("login"), 24, TimeUnit.HOUR)
                                        .timezone(field("location")))
                                .format("%Y-%m-%d %H:%M"))
                        .include("startTZInfo", dateToString()
                                .date(field("login"))
                                .format("%Y-%m-%d %H:%M")
                                .timeZone(field("location")))
                        .include("daysTZInfo", dateToString()
                                .date(dateAdd(field("login"), 1, TimeUnit.DAY)
                                        .timezone(field("location")))
                                .format("%Y-%m-%d %H:%M")
                                .timeZone(field("location")))
                        .include("hoursTZInfo", dateToString()
                                .date(dateAdd(field("login"), 24, TimeUnit.HOUR)
                                        .timezone(field("location")))
                                .format("%Y-%m-%d %H:%M")
                                .timeZone(field("location"))))

        );

    }
}

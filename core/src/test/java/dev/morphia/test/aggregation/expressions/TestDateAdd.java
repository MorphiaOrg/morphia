package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.TimeUnit;
import dev.morphia.test.DriverVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.expressions.DateExpressions.dateAdd;
import static dev.morphia.aggregation.expressions.DateExpressions.dateToString;
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
                                dateAdd("$purchaseDate", 3, TimeUnit.DAY))));

    }

    @Test
    public void testExample2() {
        testPipeline(ANY, false, true, aggregation -> aggregation.pipeline(
                match(expr(
                        gt("$deliveryDate", dateAdd("$purchaseDate", 5, TimeUnit.DAY)))),

                project()
                        .suppressId()
                        .include("custId")
                        .include("purchased", dateToString()
                                .date("$purchaseDate")
                                .format("%Y-%m-%d"))
                        .include("delivery", dateToString()
                                .date("$deliveryDate")
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
                                .date("$login")
                                .format("%Y-%m-%d %H:%M"))
                        .include("days", dateToString()
                                .date(dateAdd("$login", 1, TimeUnit.DAY)
                                        .timezone("$location"))
                                .format("%Y-%m-%d %H:%M"))
                        .include("hours", dateToString()
                                .date(dateAdd("$login", 24, TimeUnit.HOUR)
                                        .timezone("$location"))
                                .format("%Y-%m-%d %H:%M"))
                        .include("startTZInfo", dateToString()
                                .date("$login")
                                .format("%Y-%m-%d %H:%M")
                                .timeZone("$location"))
                        .include("daysTZInfo", dateToString()
                                .date(dateAdd("$login", 1, TimeUnit.DAY)
                                        .timezone("$location"))
                                .format("%Y-%m-%d %H:%M")
                                .timeZone("$location"))
                        .include("hoursTZInfo", dateToString()
                                .date(dateAdd("$login", 24, TimeUnit.HOUR)
                                        .timezone("$location"))
                                .format("%Y-%m-%d %H:%M")
                                .timeZone("$location")))

        );

    }
}

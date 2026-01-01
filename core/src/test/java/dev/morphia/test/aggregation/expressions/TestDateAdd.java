package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.TimeUnit;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.gt;
import static dev.morphia.aggregation.expressions.DateExpressions.dateAdd;
import static dev.morphia.aggregation.expressions.DateExpressions.dateToString;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Merge.merge;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.query.filters.Filters.expr;

public class TestDateAdd extends TemplatedTestBase {

    @BeforeMethod
    public void versionCheck() {
        checkMinServerVersion("5.0.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/dateAdd/example1
     * 
     */
    @Test(testName = "Add a Future Date")
    public void testExample1() {
        testPipeline(new ActionTestOptions().removeIds(true),
                aggregation -> aggregation.pipeline(
                        project().include("expectedDeliveryDate", dateAdd("$purchaseDate", 3, TimeUnit.DAY)),
                        merge(EXAMPLE_TEST_COLLECTION)));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/dateAdd/example2
     * 
     */
    @Test(testName = "Filter on a Date Range")
    public void testExample2() {
        testPipeline(aggregation -> aggregation.pipeline(
                match(expr(gt("$deliveryDate", dateAdd("$purchaseDate", 5, TimeUnit.DAY)))),

                project().suppressId().include("custId")
                        .include("purchased", dateToString().date("$purchaseDate").format("%Y-%m-%d"))
                        .include("delivery", dateToString().date("$deliveryDate").format("%Y-%m-%d"))

        ));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/dateAdd/example3
     * 
     */
    @Test(testName = "Adjust for Daylight Savings Time")
    public void testExample3() {
        testPipeline(aggregation -> aggregation.pipeline(project().suppressId().include("location")
                .include("start", dateToString().date("$login").format("%Y-%m-%d %H:%M"))
                .include("days",
                        dateToString().date(dateAdd("$login", 1, TimeUnit.DAY).timezone("$location"))
                                .format("%Y-%m-%d %H:%M"))
                .include("hours",
                        dateToString().date(dateAdd("$login", 24, TimeUnit.HOUR).timezone("$location"))
                                .format("%Y-%m-%d %H:%M"))
                .include("startTZInfo", dateToString().date("$login").format("%Y-%m-%d %H:%M").timeZone("$location"))
                .include("daysTZInfo",
                        dateToString().date(dateAdd("$login", 1, TimeUnit.DAY).timezone("$location"))
                                .format("%Y-%m-%d %H:%M").timeZone("$location"))
                .include("hoursTZInfo", dateToString().date(dateAdd("$login", 24, TimeUnit.HOUR).timezone("$location"))
                        .format("%Y-%m-%d %H:%M").timeZone("$location")))

        );

    }
}

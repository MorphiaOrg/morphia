package dev.morphia.test.aggregation.stages;

import java.time.LocalDate;
import java.time.Month;

import dev.morphia.aggregation.expressions.AccumulatorExpressions;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.DateExpressions.dateToString;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.lt;

public class TestGroup extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/group/example1
     * 
     */
    @Test(testName = "Count the Number of Documents in a Collection")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation.pipeline(group(id(null)).field("count", AccumulatorExpressions.count())));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/group/example2
     * 
     */
    @Test(testName = "Retrieve Distinct Values")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(false),
                (aggregation) -> aggregation.pipeline(group(id("$item"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/group/example3
     * 
     */
    @Test(testName = "Group by Item Having")
    public void testExample3() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(false),
                (aggregation) -> aggregation.pipeline(
                        group(id("$item")).field("totalSaleAmount", sum(multiply("$price", "$quantity"))),
                        match(gte("totalSaleAmount", 100))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/group/example4
     * 
     */
    @Test(testName = "Calculate Count, Sum, and Average")
    public void testExample4() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation.pipeline(
                        match(gte("date", LocalDate.of(2014, Month.JANUARY, 1)),
                                lt("date", LocalDate.of(2015, Month.JANUARY, 1))),
                        group(id(dateToString().date("$date").format("%Y-%m-%d")))
                                .field("totalSaleAmount", sum(multiply("$price", "$quantity")))
                                .field("averageQuantity", avg("$quantity")).field("count", sum(1)),
                        sort().descending("totalSaleAmount")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/group/example5
     * 
     */
    @Test(testName = "Pivot Data")
    public void testExample5() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(false),
                (aggregation) -> aggregation.pipeline(group(id("$author")).field("books", push("$title"))));
    }

}

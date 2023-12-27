package dev.morphia.test.aggregation.stages;

import java.time.LocalDate;
import java.time.Month;

import dev.morphia.aggregation.expressions.AccumulatorExpressions;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

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

public class TestGroup extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                group(id(null))
                        .field("count", AccumulatorExpressions.count())));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                group(id("$item"))));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                group(id("$item"))
                        .field("totalSaleAmount", sum(multiply("$price", "$quantity"))),
                match(gte("totalSaleAmount", 100))));
    }

    @Test
    public void testExample4() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                match(
                        gte("date", LocalDate.of(2014, Month.JANUARY, 1)),
                        lt("date", LocalDate.of(2015, Month.JANUARY, 1))),
                group(id(dateToString()
                        .date("$date")
                        .format("%Y-%m-%d")))
                        .field("totalSaleAmount", sum(multiply("$price", "$quantity")))
                        .field("averageQuantity", avg("$quantity"))
                        .field("count", sum(1)),
                sort()
                        .descending("totalSaleAmount")));
    }

    @Test
    public void testExample5() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                group(id("$author"))
                        .field("books", push("$title"))));
    }

}

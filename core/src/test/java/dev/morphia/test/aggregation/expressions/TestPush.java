package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.DateExpressions.dayOfYear;
import static dev.morphia.aggregation.expressions.DateExpressions.year;
import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.*;
import static dev.morphia.aggregation.stages.SetWindowFields.setWindowFields;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.Sort.*;

public class TestPush extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                sort()
                        .ascending("date")
                        .ascending("item"),
                group(id()
                        .field("day",
                                dayOfYear("$date"))
                        .field("year",
                                year("$date")))
                        .field("itemsSold", push(document()
                                .field("item", "$item")
                                .field("quantity", "$quantity")))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                setWindowFields()
                        .partitionBy("$state")
                        .sortBy(ascending("orderDate"))
                        .output(output("quantitiesForState")
                                .operator(push("$quantity"))
                                .window()
                                .documents("unbounded", "current"))));
    }

}

package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.DateExpressions.dateTrunc;
import static dev.morphia.aggregation.expressions.TimeUnit.*;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Projection.project;
import static java.time.DayOfWeek.*;

public class TestDateTrunc extends AggregationTest {
    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("orderDate")
                        .include("truncatedOrderDate", dateTrunc("$orderDate",
                                WEEK).binSize(2)
                                .timezone("America/Los_Angeles")
                                .startOfWeek(MONDAY))));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                group(id().field("truncatedOrderDate", dateTrunc("$orderDate", MONTH).binSize(6)))
                        .field("sumQuantity", sum("$quantity"))));
    }

}

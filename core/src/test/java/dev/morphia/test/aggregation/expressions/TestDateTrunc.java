package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.DateExpressions.dateTrunc;
import static dev.morphia.aggregation.expressions.TimeUnit.*;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static java.time.DayOfWeek.*;

public class TestDateTrunc extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.0.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/dateTrunc/truncateOrderDatesAndObtainQuantitySumInAGroupPipelineStage
     * 
     */
    @Test
    @DisplayName("Truncate Order Dates and Obtain Quantity Sum in a ``$group`` Pipeline Stage")
    public void testTruncateOrderDatesAndObtainQuantitySumInAGroupPipelineStage() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation
                        .pipeline(group(id().field("truncatedOrderDate", dateTrunc("$orderDate", MONTH).binSize(6)))
                                .field("sumQuantity", sum("$quantity"))));
    }

}

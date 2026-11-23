package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.expressions.TimeUnit;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.dateAdd;
import static dev.morphia.aggregation.stages.Merge.merge;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestDateAdd extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.0.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/dateAdd/addAFutureDate
     * 
     */
    @Test
    @DisplayName("Add a Future Date")
    public void testAddAFutureDate() {
        testPipeline(new ActionTestOptions().removeIds(true),
                aggregation -> aggregation.pipeline(
                        project().include("expectedDeliveryDate", dateAdd("$purchaseDate", 3, TimeUnit.DAY)),
                        merge("shipping")));

    }

}

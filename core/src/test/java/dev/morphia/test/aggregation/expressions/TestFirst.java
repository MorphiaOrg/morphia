package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.first;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Sort.sort;

public class TestFirst extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/aggregation/expressions/first/useInGroupStage
     * 
     */
    @Test
    @DisplayName("Use in ``$group`` Stage")
    public void testUseInGroupStage() {
        testPipeline(new ActionTestOptions().orderMatters(false), (aggregation) -> aggregation
                .pipeline(sort().ascending("item", "date"), group(id("$item")).field("firstSale", first("$date"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/first/missingData
     * 
     */
    @Test
    @DisplayName("Missing Data")
    public void testMissingData() {
        testPipeline(new ActionTestOptions().orderMatters(false), (aggregation) -> aggregation
                .pipeline(sort().ascending("item", "price"), group(id("$item")).field("inStock", first("$quantity"))

                ));
    }

}

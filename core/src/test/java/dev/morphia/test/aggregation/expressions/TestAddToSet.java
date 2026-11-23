package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.stages.Group;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.addToSet;
import static dev.morphia.aggregation.expressions.DateExpressions.dayOfYear;
import static dev.morphia.aggregation.expressions.DateExpressions.year;
import static dev.morphia.aggregation.stages.Group.group;

public class TestAddToSet extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/addToSet/useInGroupStage
     * 
     */
    @Test
    @DisplayName("Use in ``$group`` Stage")
    public void testUseInGroupStage() {
        testPipeline(new ActionTestOptions().orderMatters(false),
                aggregation -> aggregation
                        .pipeline(group(Group.id().field("day", dayOfYear("$date")).field("year", year("$date")))
                                .field("itemsSold", addToSet("$item")))

        );

    }

}

package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.stages.Group;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.addToSet;
import static dev.morphia.aggregation.expressions.DateExpressions.dayOfYear;
import static dev.morphia.aggregation.expressions.DateExpressions.year;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Limit.limit;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.aggregation.stages.Unwind.unwind;

public class TestAddToSet extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/addToSet/useInGroupStage
     * 
     */
    @Test
    @DisplayName("Use in ``$group`` Stage")
    public void testUseInGroupStage() {
        testPipeline(aggregation -> aggregation
                .pipeline(group(Group.id().field("day", dayOfYear("$date")).field("year", year("$date")))
                        .field("itemsSold", addToSet("$item")))

        );

    }

    @Test
    @DisplayName("Collect the Genres of Each Director")
    public void testCollectTheGenresOfEachDirector() {
        testPipeline((aggregation) -> aggregation.pipeline(
                unwind("$directors"),
                unwind("$genres"),
                group(id("$directors")).field("genres", addToSet("$genres")),
                sort().ascending("_id"),
                limit(5)));
    }
}

package dev.morphia.test.aggregation.expressions;

import dev.morphia.query.Type;
import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.last;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Limit.limit;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.SetWindowFields.Output.*;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.aggregation.stages.Unwind.unwind;
import static dev.morphia.query.Sort.*;
import static dev.morphia.query.filters.Filters.type;

public class TestLast extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/last/useInGroupStage
     * 
     */
    @Test
    @DisplayName("Use in ``$group`` Stage")
    public void testUseInGroupStage() {
        testPipeline((aggregation) -> aggregation.pipeline(sort().ascending("item").ascending("date"),
                group(id("$item")).field("lastSalesDate", last("$date"))));
    }

    @Test
    @DisplayName("Find the Last Movie in Each Genre")
    public void testFindTheLastMovieInEachGenre() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(type("year", Type.INTEGER_32_BIT)),
                sort().ascending("year").ascending("title"),
                unwind("$genres"),
                group(id("$genres")).field("lastMovieTitle", last("$title")).field("lastMovieYear", last("$year")),
                sort().ascending("_id"),
                limit(5)));
    }
}

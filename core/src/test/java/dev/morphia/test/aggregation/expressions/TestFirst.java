package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.first;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Limit.limit;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.aggregation.stages.Unwind.unwind;

public class TestFirst extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/aggregation/expressions/first/useInGroupStage
     * 
     */
    @Test
    @DisplayName("Use in ``$group`` Stage")
    public void testUseInGroupStage() {
        testPipeline((aggregation) -> aggregation
                .pipeline(sort().ascending("item", "date"), group(id("$item")).field("firstSale", first("$date"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/first/missingData
     * 
     */
    @Test
    @DisplayName("Missing Data")
    public void testMissingData() {
        testPipeline((aggregation) -> aggregation
                .pipeline(sort().ascending("item", "price"), group(id("$item")).field("inStock", first("$quantity"))

                ));
    }

    @Test
    @DisplayName("Find the First Movie in Each Genre")
    public void testFindTheFirstMovieInEachGenre() {
        testPipeline((aggregation) -> aggregation.pipeline(
                sort().ascending("year").ascending("title"),
                unwind("$genres"),
                group(id("$genres")).field("firstMovieTitle", first("$title")).field("firstMovieYear", first("$year")),
                sort().ascending("_id"),
                limit(5)));
    }
}

package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;

import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.stages.Limit.limit;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.filters.Filters.text;

public class TestSort extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/sort/ascendingDescendingSort
     * 
     */
    @Test
    @DisplayName("Ascending/Descending Sort")
    public void testAscendingDescendingSort() {
        testPipeline((aggregation) -> aggregation.pipeline(sort().descending("age").ascending("posts")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/sort/textScoreMetadataSort
     * 
     */
    @Test
    @DisplayName("Text Score Metadata Sort")
    public void testTextScoreMetadataSort() {
        getDatabase().getCollection(EXAMPLE_TEST_COLLECTION).createIndex(new Document("$**", "text"));
        testPipeline((aggregation) -> aggregation
                .pipeline(match(text("operating")), sort().meta("score").descending("posts")));
    }

    @Test
    @DisplayName("Ascending and Descending Sort")
    public void testAscendingAndDescendingSort() {
        testPipeline((aggregation) -> aggregation.pipeline(
                sort().descending("year").ascending("title"),
                limit(5)));
    }
}

package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.filters.Filters.text;

public class TestSort extends AggregationTest {
    public TestSort() {
        skipDataCheck();
    }

    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                sort()
                        .descending("age")
                        .ascending("posts")));
    }

    @Test
    public void testExample2() {
        getDatabase().getCollection(EXAMPLE_TEST_COLLECTION)
                .createIndex(new Document("$**", "text"));
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                match(text("operating")),
                sort()
                        .meta("score")
                        .descending("posts")));
    }

}

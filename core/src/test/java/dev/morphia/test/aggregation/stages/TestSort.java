package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Sort.sort;
import static dev.morphia.query.filters.Filters.text;

public class TestSort extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/sort/example1
     * 
     */
    @Test(testName = "Ascending/Descending Sort")
    public void testExample1() {
        testPipeline(new ActionTestOptions().skipDataCheck(true),
                (aggregation) -> aggregation.pipeline(sort().descending("age").ascending("posts")));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/sort/example2
     * 
     */
    @Test(testName = "Text Score Metadata Sort")
    public void testExample2() {
        getDatabase().getCollection(EXAMPLE_TEST_COLLECTION).createIndex(new Document("$**", "text"));
        testPipeline(new ActionTestOptions().skipDataCheck(true), (aggregation) -> aggregation
                .pipeline(match(text("operating")), sort().meta("score").descending("posts")));
    }

}

package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.stages.AutoBucket.autoBucket;
import static dev.morphia.aggregation.stages.Documents.documents;
import static dev.morphia.aggregation.stages.Lookup.lookup;
import static dev.morphia.aggregation.stages.Match.match;

public class TestDocuments extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/documents/example1
     * 
     */
    @Test(testName = "Test a Pipeline Stage")
    public void testExample1() {
        testPipeline(new ActionTestOptions().skipDataCheck(true),
                (aggregation) -> aggregation.pipeline(
                        documents(document().field("x", 10), document().field("x", 2), document().field("x", 5)),
                        autoBucket().groupBy("$x").buckets(4)));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/documents/example2
     * 
     */
    @Test(testName = "Use a ``$documents`` Stage in a ``$lookup`` Stage")
    public void testExample2() {
        testPipeline(new ActionTestOptions().removeIds(true), aggregation -> {
            return aggregation.pipeline(match(),
                    lookup().localField("zip").foreignField("zip_id").as("city_state")
                            .pipeline(documents(document("zip_id", 94301).field("name", "Palo Alto, CA"),
                                    document("zip_id", 10019).field("name", "New York, NY"))));

        });
    }
}

package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.document;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.stages.AutoBucket.autoBucket;
import static dev.morphia.aggregation.stages.Documents.documents;
import static dev.morphia.aggregation.stages.Lookup.lookup;
import static dev.morphia.test.ServerVersion.v51;

public class TestDocuments extends AggregationTest {
    @Test
    public void testExample1() {
        skipDataCheck();
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                documents(
                        document().field("x", value(10)),
                        document().field("x", value(2)),
                        document().field("x", value(5))),
                autoBucket()
                        .groupBy(field("x"))
                        .buckets(4)));
    }

    @Test
    public void testExample2() {
        testPipeline(v51, aggregation -> {
            return aggregation
                    .match()
                    .lookup(lookup()
                            .localField("zip")
                            .foreignField("zip_id")
                            .as("city_state")
                            .pipeline(
                                    documents(
                                            document("zip_id", value(94301))
                                                    .field("name", value("Palo Alto, CA")),
                                            document("zip_id", value(10019))
                                                    .field("name", value("New York, NY")))));

        });
    }
}

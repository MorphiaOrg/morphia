package dev.morphia.test.aggregation.stages;

import dev.morphia.aggregation.stages.Match;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.stages.AutoBucket.autoBucket;
import static dev.morphia.aggregation.stages.Bucket.bucket;
import static dev.morphia.aggregation.stages.Facet.facet;
import static dev.morphia.aggregation.stages.SortByCount.sortByCount;
import static dev.morphia.aggregation.stages.Unwind.unwind;
import static dev.morphia.query.filters.Filters.exists;

public class TestFacet extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                facet()
                        .field("categorizedByTags",
                                unwind("tags"),
                                sortByCount("$tags"))
                        .field("categorizedByPrice",
                                Match.match(exists("price")),
                                bucket()
                                        .groupBy("$price")
                                        .boundaries(0, 150, 200, 300, 400)
                                        .defaultValue("Other")
                                        .outputField("count", sum(1))
                                        .outputField("titles", push().single("$title")))
                        .field("categorizedByYears(Auto)", autoBucket()
                                .groupBy("$year")
                                .buckets(4))));
    }
}

package dev.morphia.test.aggregation.stages;

import dev.morphia.aggregation.stages.Match;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
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
                                sortByCount(field("tags")))
                        .field("categorizedByPrice",
                                Match.match(exists("price")),
                                bucket()
                                        .groupBy(field("price"))
                                        .boundaries(value(0), value(150), value(200), value(300), value(400))
                                        .defaultValue("Other")
                                        .outputField("count", sum(value(1)))
                                        .outputField("titles", push().single(field("title"))))
                        .field("categorizedByYears(Auto)", autoBucket()
                                .groupBy(field("year"))
                                .buckets(4))));
    }
}

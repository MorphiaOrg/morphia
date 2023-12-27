package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.Expressions.meta;
import static dev.morphia.aggregation.expressions.MetadataKeyword.*;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.text;

public class TestMeta extends AggregationTest {
    @Test
    public void testExample1() {
        getDatabase().getCollection(AGG_TEST_COLLECTION)
                .createIndex(new Document("title", "text"));
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                match(text("cake")),
                group(id(meta()))
                        .field("count", sum(1))));
    }

    @Test
    public void testExample2() {
        getDatabase().getCollection(AGG_TEST_COLLECTION)
                .createIndex(new Document("type", 1).append("item", 1));

        testPipeline(ServerVersion.ANY, true, true, (aggregation) -> aggregation.pipeline(
                match(eq("type", "apparel")),
                addFields()
                        .field("idxKey", meta(INDEXKEY))));
    }

}

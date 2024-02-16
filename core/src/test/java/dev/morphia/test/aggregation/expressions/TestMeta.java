package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.Expressions.meta;
import static dev.morphia.aggregation.expressions.MetadataKeyword.*;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.text;

public class TestMeta extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                match(text("cake")),
                group(id(meta()))
                        .field("count", sum(1))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, true, true, (aggregation) -> aggregation.pipeline(
                match(eq("type", "apparel")),
                addFields()
                        .field("idxKey", meta(INDEXKEY))));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, true, true, (aggregation) -> aggregation.pipeline(
                match(gte("price", 10.0)),
                addFields()
                        .field("idxKey", meta(INDEXKEY))));
    }

}

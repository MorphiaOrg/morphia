package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.stages.Group.group;
import static dev.morphia.aggregation.stages.Group.id;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.and;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gt;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.lt;
import static dev.morphia.query.filters.Filters.or;

public class TestMatch extends AggregationTest {
    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                match(eq("author", "dave"))));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, true, false, (aggregation) -> aggregation.pipeline(
                match(or(
                        and(
                                gt("score", 70),
                                lt("score", 90)),
                        gte("views", 1000))),
                group(id(null))
                        .field("count", sum(1))));
    }

}

package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

public class TestMatch extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/match/equalityMatch
     */
    @Test
    @DisplayName("Equality Match")
    public void testEqualityMatch() {
        testPipeline(aggregation -> aggregation.pipeline(match(eq("author", "dave"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/match/performACount
     *
     */
    @Test
    @DisplayName("Perform a Count")
    public void testPerformACount() {
        testPipeline((aggregation) -> aggregation.pipeline(
                match(or(and(gt("score", 70), lt("score", 90)), gte("views", 1000))),
                group(id(null)).field("count", sum(1))));
    }

}

package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

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

public class TestMatch extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/match/example1
     */
    @Test(testName = "Equality Match")
    public void testExample1() {
        testPipeline(aggregation -> aggregation.pipeline(match(eq("author", "dave"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/match/example2
     *
     */
    @Test(testName = "Perform a Count")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion("0.0.0").removeIds(true).orderMatters(false),
                (aggregation) -> aggregation.pipeline(
                        match(or(and(gt("score", 70), lt("score", 90)), gte("views", 1000))),
                        group(id(null)).field("count", sum(1))));
    }

}

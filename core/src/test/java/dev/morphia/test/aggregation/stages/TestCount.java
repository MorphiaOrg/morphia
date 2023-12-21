package dev.morphia.test.aggregation.stages;

import dev.morphia.aggregation.stages.Count;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.Count.*;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.gt;

public class TestCount extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                match(gt("score", 80)), Count.count("passing_scores")));
    }

}

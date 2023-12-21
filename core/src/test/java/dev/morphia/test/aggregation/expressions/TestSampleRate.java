package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.stages.Count;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Miscellaneous.sampleRate;
import static dev.morphia.aggregation.stages.Count.*;
import static dev.morphia.aggregation.stages.Match.match;

public class TestSampleRate extends AggregationTest {
    @Test
    public void testExample1() {
        skipDataCheck();
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                match(sampleRate(0.33)),
                Count.count("numMatches")));
    }

}

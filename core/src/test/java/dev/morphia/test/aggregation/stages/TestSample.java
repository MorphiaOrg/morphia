package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.Sample.sample;

public class TestSample extends AggregationTest {
    @Test
    public void testExample1() {
        skipDataCheck(); // the results are random
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                sample(3)));
    }
}

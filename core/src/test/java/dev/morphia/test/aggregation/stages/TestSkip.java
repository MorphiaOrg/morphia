package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.Skip.skip;

public class TestSkip extends AggregationTest {
    @Test
    public void testExample1() {
        skipDataCheck();
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                skip(5)));
    }
}

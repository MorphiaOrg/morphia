package dev.morphia.test.aggregation.expressions;

import dev.morphia.aggregation.stages.Count;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Miscellaneous.sampleRate;
import static dev.morphia.aggregation.stages.Count.*;
import static dev.morphia.aggregation.stages.Match.match;

public class TestSampleRate extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/sampleRate/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(
                new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true)
                        .skipDataCheck(true),
                (aggregation) -> aggregation.pipeline(match(sampleRate(0.33)), Count.count("numMatches")));
    }

}

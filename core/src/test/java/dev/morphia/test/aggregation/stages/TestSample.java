package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.stages.Sample.sample;

public class TestSample extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/sample/sampleAggregationStage
     * 
     */
    @Test
    @DisplayName("main")
    public void testSampleAggregationStage() {
        testPipeline((aggregation) -> aggregation.pipeline(sample(3)));
    }
}

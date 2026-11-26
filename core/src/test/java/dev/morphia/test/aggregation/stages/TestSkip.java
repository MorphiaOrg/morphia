package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.stages.Skip.skip;

public class TestSkip extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/skip/skipAggregationStage
     * 
     */
    @Test
    @DisplayName("main")
    public void testSkipAggregationStage() {
        testPipeline((aggregation) -> aggregation.pipeline(skip(5)));
    }
}

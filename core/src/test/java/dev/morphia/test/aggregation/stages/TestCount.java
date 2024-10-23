package dev.morphia.test.aggregation.stages;

import dev.morphia.aggregation.stages.Count;
import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.Count.*;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.query.filters.Filters.gt;

public class TestCount extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/count/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(match(gt("score", 80)), Count.count("passing_scores")));
    }

}

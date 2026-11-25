package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.stages.Limit.limit;

public class TestLimit extends TemplatedTestBase {

    @Test
    @DisplayName("Return the First Five Movies")
    public void testReturnTheFirstFiveMovies() {
        testPipeline((aggregation) -> aggregation.pipeline(
                limit(5)));
    }
}

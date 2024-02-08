package dev.morphia.test.aggregation.stages;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.SortByCount.sortByCount;
import static dev.morphia.aggregation.stages.Unwind.unwind;

public class TestSortByCount extends AggregationTest {
    @Test
    public void testExample1() {
        // orderMatters is false here because of the indeterminate sort order on equal values
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation.pipeline(
                unwind("$tags"),
                sortByCount("$tags")));
    }
}

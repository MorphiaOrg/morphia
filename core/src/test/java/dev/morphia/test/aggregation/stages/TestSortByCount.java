package dev.morphia.test.aggregation.stages;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.stages.SortByCount.sortByCount;
import static dev.morphia.aggregation.stages.Unwind.unwind;

public class TestSortByCount extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/stages/sortByCount/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        // orderMatters is false here because of the indeterminate sort order on equal
        // values
        testPipeline(new ActionTestOptions().orderMatters(false),
                (aggregation) -> aggregation.pipeline(unwind("$tags"), sortByCount("$tags")));
    }
}

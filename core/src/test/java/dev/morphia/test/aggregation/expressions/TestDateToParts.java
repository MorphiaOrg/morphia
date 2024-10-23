package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.dateToParts;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestDateToParts extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/dateToParts/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(
                project().include("date", dateToParts("$date")).include("date_iso", dateToParts("$date").iso8601(true))
                        .include("date_timezone", dateToParts("$date").timezone("America/New_York"))));
    }

}

package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.dateToParts;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestDateToParts extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("date", dateToParts("$date"))
                        .include("date_iso", dateToParts("$date")
                                .iso8601(true))
                        .include("date_timezone", dateToParts("$date")
                                .timezone("America/New_York"))));
    }

}

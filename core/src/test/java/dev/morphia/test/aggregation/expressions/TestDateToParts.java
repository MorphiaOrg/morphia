package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.dateToParts;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.Expressions.value;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestDateToParts extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("date", dateToParts(field("date")))
                        .include("date_iso", dateToParts(field("date"))
                                .iso8601(true))
                        .include("date_timezone", dateToParts(field("date"))
                                .timezone(value("America/New_York")))));
    }

}

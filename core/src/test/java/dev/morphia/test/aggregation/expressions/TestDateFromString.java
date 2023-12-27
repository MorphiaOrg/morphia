package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DateExpressions.dateFromString;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestDateFromString extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("date", dateFromString()
                                .dateString("$date")
                                .timeZone("$timezone"))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("date", dateFromString()
                                .dateString("$date")
                                .timeZone("$timezone")
                                .onError("$date"))));
    }

    @Test
    public void testExample3() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("date", dateFromString()
                                .dateString("$date")
                                .timeZone("$timezone")
                                .onNull("oops"))

        ));
    }

}

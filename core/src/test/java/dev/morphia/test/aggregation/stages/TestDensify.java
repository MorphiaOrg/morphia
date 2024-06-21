package dev.morphia.test.aggregation.stages;

import java.time.ZonedDateTime;

import dev.morphia.aggregation.stages.Densify.Range;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.TimeUnit.HOUR;
import static dev.morphia.aggregation.stages.Densify.densify;

public class TestDensify extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/stages/densify/example1
     * 
     */
    @Test(testName = "Densify Time Series Data")
    public void testExample1() {
        testPipeline(
                ServerVersion.v51, true, true, (
                        aggregation) -> aggregation
                                .pipeline(
                                        densify("timestamp",
                                                Range.bounded(ZonedDateTime.parse("2021-05-18T00:00:00.000Z"),
                                                        ZonedDateTime.parse("2021-05-18T08:00:00.000Z"), 1)
                                                        .unit(HOUR))));
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/densify/example2
     * 
     */
    @Test(testName = "Densifiction with Partitions")
    public void testExample2() {
        testPipeline(ServerVersion.v51, true, false, (aggregation) -> aggregation
                .pipeline(densify("altitude", Range.full(200)).partitionByFields("variety")));
    }

}
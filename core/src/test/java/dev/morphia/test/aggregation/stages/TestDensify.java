package dev.morphia.test.aggregation.stages;

import java.time.ZonedDateTime;

import dev.morphia.aggregation.stages.Densify.Range;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.TimeUnit.HOUR;
import static dev.morphia.aggregation.stages.Densify.densify;

public class TestDensify extends TemplatedTestBase {

    @BeforeEach
    public void versionCheck() {
        checkMinServerVersion("5.1.0");
    }

    /**
     * test data: dev/morphia/test/aggregation/stages/densify/densifyTimeSeriesData
     * 
     */
    @Test
    @DisplayName("Densify Time Series Data")
    public void testDensifyTimeSeriesData() {
        testPipeline(
                new ActionTestOptions().removeIds(true), (
                        aggregation) -> aggregation
                                .pipeline(
                                        densify("timestamp",
                                                Range.bounded(ZonedDateTime.parse("2021-05-18T00:00:00.000Z"),
                                                        ZonedDateTime.parse("2021-05-18T08:00:00.000Z"), 1)
                                                        .unit(HOUR))));
    }

}
package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DataSizeExpressions.binarySize;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.stages.Limit.limit;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.Sort.sort;

public class TestBinarySize extends AggregationTest {
    @Test
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("name", field("name"))
                        .include("imageSize", binarySize(field("binary")))));
    }

    @Test
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true, (aggregation) -> aggregation.pipeline(
                project()
                        .include("name", field("name"))
                        .include("imageSize", binarySize(field("binary"))),
                sort().descending("imageSize"),
                limit(1)));
    }

}

package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.DataSizeExpressions.binarySize;
import static dev.morphia.aggregation.stages.Limit.limit;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.Sort.sort;

public class TestBinarySize extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/binarySize/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation
                        .pipeline(project().include("name", "$name").include("imageSize", binarySize("$binary"))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/binarySize/example2
     * 
     */
    @Test(testName = "Find Largest Binary Data")
    public void testExample2() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation.pipeline(
                        project().include("name", "$name").include("imageSize", binarySize("$binary")),
                        sort().descending("imageSize"), limit(1)));
    }

}

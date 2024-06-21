package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.ltrim;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestLtrim extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/ltrim/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation
                        .pipeline(project().include("item").include("description", ltrim("$description"))));
    }

}

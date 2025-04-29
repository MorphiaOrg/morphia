package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.TypeExpressions.toUuid;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestToUUID extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/aggregation/expressions/toUUID/example1
     */
    @Test(testName = "main")
    public void testExample1() {
        checkMinServerVersion("8.0.0");
        testPipeline(new ActionTestOptions().removeIds(true), aggregation -> aggregation
                .pipeline(project().include("name").include("price").include("UUID", toUuid("$UUID"))));
    }
}
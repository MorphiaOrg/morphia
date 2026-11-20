package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.TypeExpressions.toUuid;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestToUUID extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/aggregation/expressions/toUUID/toUuidExpressionOperator
     */
    @Test
    @DisplayName("main")
    public void testToUuidExpressionOperator() {
        checkMinServerVersion("8.0.0");
        testPipeline(new ActionTestOptions().removeIds(true), aggregation -> aggregation
                .pipeline(project().include("name").include("price").include("UUID", toUuid("$UUID"))));
    }
}
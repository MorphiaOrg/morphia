package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.SetExpressions.setEquals;
import static dev.morphia.aggregation.stages.Projection.project;

public class TestSetEquals extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/setEquals/example1
     * 
     */
    @Test
    @DisplayName("main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(project().suppressId().include("cakes").include("cupcakes")
                .include("sameFlavors", setEquals("$cakes", "$cupcakes"))));
    }

}

package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.TrigonometryExpressions.degreesToRadians;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestDegreesToRadians extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/degreesToRadians/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(true).orderMatters(true),
                (aggregation) -> aggregation.pipeline(addFields().field("angle_a_rad", degreesToRadians("$angle_a"))
                        .field("angle_b_rad", degreesToRadians("$angle_b"))
                        .field("angle_c_rad", degreesToRadians("$angle_c"))));
    }

}

package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.TrigonometryExpressions.radiansToDegrees;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestRadiansToDegrees extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/radiansToDegrees/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(true).orderMatters(true),
                (aggregation) -> aggregation.pipeline(addFields().field("angle_a_deg", radiansToDegrees("$angle_a"))
                        .field("angle_b_deg", radiansToDegrees("$angle_b"))
                        .field("angle_c_deg", radiansToDegrees("$angle_c"))));
    }

}

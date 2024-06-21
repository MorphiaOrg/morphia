package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.DriverVersion;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.Expressions.literal;
import static dev.morphia.aggregation.expressions.Miscellaneous.setField;
import static dev.morphia.aggregation.expressions.SystemVariables.*;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.ReplaceWith.replaceWith;
import static dev.morphia.aggregation.stages.Unset.unset;
import static dev.morphia.query.filters.Filters.eq;

public class TestSetField extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/setField/example1
     * 
     */
    @Test(testName = "Add Fields that Contain Periods (``.``)")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.v50).minDriver(DriverVersion.v43),
                (aggregation) -> aggregation.pipeline(replaceWith(setField("price.usd", ROOT, "$price")),
                        unset("price")));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/setField/example2
     * 
     */
    @Test(testName = "Add Fields that Start with a Dollar Sign (``$``)")
    public void testExample2() {
        testPipeline(new ActionTestOptions().minDriver(DriverVersion.v43), (aggregation) -> aggregation
                .pipeline(replaceWith(setField(literal("$price"), ROOT, "$price")), unset("price")));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/setField/example3
     * 
     */
    @Test(testName = "Update Fields that Contain Periods (``.``)")
    public void testExample3() {
        testPipeline(new ActionTestOptions().minDriver(DriverVersion.v43), (aggregation) -> aggregation
                .pipeline(match(eq("_id", 1)), replaceWith(setField("price.usd", ROOT, 49.99))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/setField/example4
     * 
     */
    @Test(testName = "Update Fields that Start with a Dollar Sign (``$``)")
    public void testExample4() {
        testPipeline(new ActionTestOptions().minDriver(DriverVersion.v43), (aggregation) -> aggregation
                .pipeline(match(eq("_id", 1)), replaceWith(setField(literal("$price"), ROOT, 49.99))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/setField/example5
     * 
     */
    @Test(testName = "Remove Fields that Contain Periods (``.``)")
    public void testExample5() {
        testPipeline((aggregation) -> aggregation.pipeline(replaceWith(setField("price.usd", ROOT, REMOVE))));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/setField/example6
     * 
     */
    @Test(testName = "Remove Fields that Start with a Dollar Sign (``$``)")
    public void testExample6() {
        testPipeline((aggregation) -> aggregation.pipeline(replaceWith(setField(literal("$price"), ROOT, REMOVE))));
    }

}

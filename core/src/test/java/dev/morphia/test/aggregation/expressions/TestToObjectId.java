package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.TypeExpressions.toObjectId;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Sort.sort;

public class TestToObjectId extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/toObjectId/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline((aggregation) -> aggregation.pipeline(addFields().field("convertedId", toObjectId("$_id")),
                sort().descending("convertedId")));
    }

}

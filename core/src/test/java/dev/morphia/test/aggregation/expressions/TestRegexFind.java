package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.regexFind;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestRegexFind extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/regexFind/regexFindAndItsOptions
     * 
     */
    @Test
    @DisplayName("``$regexFind`` and Its Options")
    public void testRegexFindAndItsOptions() {
        testPipeline(new ActionTestOptions().orderMatters(false), (aggregation) -> aggregation
                .pipeline(addFields().field("returnObject", regexFind("$description").pattern("line"))));
    }

}

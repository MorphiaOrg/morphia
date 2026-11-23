package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.regexMatch;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestRegexMatch extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/regexMatch/regexMatchAndItsOptions
     * 
     */
    @Test
    @DisplayName("``$regexMatch`` and Its Options")
    public void testRegexMatchAndItsOptions() {
        testPipeline(new ActionTestOptions().orderMatters(false), (aggregation) -> aggregation
                .pipeline(addFields().field("result", regexMatch("$description").pattern("line"))));

    }

}

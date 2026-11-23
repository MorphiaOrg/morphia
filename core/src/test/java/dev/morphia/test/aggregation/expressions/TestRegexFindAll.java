package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.StringExpressions.regexFindAll;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestRegexFindAll extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/regexFindAll/regexFindAllAndItsOptions
     * 
     */
    @Test
    @DisplayName("``$regexFindAll`` and Its Options")
    public void testRegexFindAllAndItsOptions() {
        testPipeline(new ActionTestOptions().orderMatters(false), (aggregation) -> aggregation
                .pipeline(addFields().field("returnObject", regexFindAll("$description").pattern("line"))));

    }

}

package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.aggregation.expressions.ArrayExpressions.array;
import static dev.morphia.aggregation.expressions.ArrayExpressions.concatArrays;
import static dev.morphia.aggregation.expressions.ArrayExpressions.reduce;
import static dev.morphia.aggregation.expressions.StringExpressions.regexFindAll;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Set.set;

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

    /**
     * test data: dev/morphia/test/aggregation/expressions/regexFindAll/useRegexFindAllToParseEmailFromString
     * 
     */
    @Test
    @DisplayName("Use ``$regexFindAll`` to Parse Email from String")
    public void testUseRegexFindAllToParseEmailFromString() {
        testPipeline((aggregation) -> aggregation.pipeline(
                addFields().field("email",
                        regexFindAll("$comment").pattern("[a-z0-9_.+-]+@[a-z0-9_.+-]+\\.[a-z0-9_.+-]+").options("i")),
                set().field("email", "$email.match")));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/regexFindAll/useCapturedGroupingsToParseUserName
     * 
     */
    @Test
    @DisplayName("Use Captured Groupings to Parse User Name")
    public void testUseCapturedGroupingsToParseUserName() {
        testPipeline(
                (aggregation) -> aggregation.pipeline(
                        addFields().field("names",
                                regexFindAll("$comment").pattern("([a-z0-9_.+-]+)@[a-z0-9_.+-]+\\.[a-z0-9_.+-]+")
                                        .options("i")),
                        set().field("names", reduce("$names.captures", array(), concatArrays("$$value", "$$this")))));
    }

}

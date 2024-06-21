package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.ServerVersion;
import dev.morphia.test.aggregation.AggregationTest;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ConditionalExpressions.condition;
import static dev.morphia.aggregation.expressions.StringExpressions.regexMatch;
import static dev.morphia.aggregation.stages.AddFields.addFields;

public class TestRegexMatch extends AggregationTest {
    /**
     * test data: dev/morphia/test/aggregation/expressions/regexMatch/example1
     * 
     */
    @Test(testName = "``$regexMatch`` and Its Options")
    public void testExample1() {
        testPipeline(ServerVersion.ANY, false, false, (aggregation) -> aggregation
                .pipeline(addFields().field("result", regexMatch("$description").pattern("line"))));

    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/regexMatch/example2
     * 
     */
    @Test(testName = "Use ``$regexMatch`` to Check Email Address")
    public void testExample2() {
        testPipeline(ServerVersion.ANY, false, true,
                (aggregation) -> aggregation.pipeline(addFields().field("category",
                        condition(regexMatch("$comment").pattern("[a-z0-9_.+-]+@mongodb.com").options("i"), "Employee",
                                "External"))));
    }

}

package dev.morphia.test.aggregation.expressions;

import dev.morphia.query.filters.Filters;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.eq;
import static dev.morphia.aggregation.expressions.ConditionalExpressions.switchExpression;
import static dev.morphia.aggregation.expressions.TypeExpressions.toBool;
import static dev.morphia.aggregation.stages.AddFields.addFields;
import static dev.morphia.aggregation.stages.Match.match;

public class TestToBool extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/toBool/example1
     * 
     */
    @Test(testName = "main")
    public void testExample1() {
        testPipeline(new ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(true),
                (aggregation) -> aggregation.pipeline(
                        addFields().field("convertedShippedFlag",
                                switchExpression().branch(eq("$shipped", "false"), false)
                                        .branch(eq("$shipped", ""), false).defaultCase(toBool("$shipped"))),
                        match(Filters.eq("convertedShippedFlag", false))));
    }

}

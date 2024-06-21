package dev.morphia.test.aggregation.expressions;

import dev.morphia.test.DriverVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.lt;
import static dev.morphia.aggregation.expressions.MathExpressions.floor;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.expressions.Miscellaneous.rand;
import static dev.morphia.aggregation.stages.Match.match;
import static dev.morphia.aggregation.stages.Merge.*;
import static dev.morphia.aggregation.stages.Projection.project;
import static dev.morphia.aggregation.stages.Set.set;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.expr;

public class TestRand extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/aggregation/expressions/rand/example1
     * 
     */
    @Test(testName = "Generate Random Data Points")
    public void testExample1() {
        testPipeline(
                new ActionTestOptions().removeIds(true).orderMatters(false).minDriver(DriverVersion.v43)
                        .skipDataCheck(true),
                (aggregation) -> aggregation.pipeline(set().field("amount", multiply(rand(), 100)),
                        set().field("amount", floor("$amount")), merge("donors")));
    }

    /**
     * test data: dev/morphia/test/aggregation/expressions/rand/example2
     * 
     */
    @Test(testName = "Select Random Items From a Collection")
    public void testExample2() {
        testPipeline(new ActionTestOptions().orderMatters(false).minDriver(DriverVersion.v43).skipDataCheck(true),
                (aggregation) -> aggregation.pipeline(match(eq("district", 3)), match(expr(lt(0.5, rand()))),
                        project().suppressId().include("name").include("registered")));
    }

}

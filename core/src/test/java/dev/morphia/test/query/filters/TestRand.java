package dev.morphia.test.query.filters;

import dev.morphia.query.FindOptions;
import dev.morphia.query.updates.UpdateOperators;
import dev.morphia.test.util.ActionTestOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.lt;
import static dev.morphia.aggregation.expressions.MathExpressions.floor;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.expressions.Miscellaneous.rand;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.expr;

public class TestRand extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/rand/example1
     */
    @Test(testName = "Generate Random Data Points")
    public void testExample1() {
        ActionTestOptions options = new ActionTestOptions().skipDataCheck(true)
                .findOptions(new FindOptions().projection().include("name", "registered"));
        testUpdate(options, (query) -> query,
                UpdateOperators.set("amount", floor(multiply(rand(), 100))));
    }

    /**
     * test data: dev/morphia/test/query/filters/rand/example2
     */
    @Test(testName = "Select Random Items From a Collection")
    public void testExample2() {
        ActionTestOptions options = new ActionTestOptions().orderMatters(false).removeIds(true).skipDataCheck(true)
                .findOptions(new FindOptions().projection().include("name", "registered"));
        testQuery(options, (query) -> query.filter(eq("district", 3), expr(lt(0.5, rand()))));
    }
}
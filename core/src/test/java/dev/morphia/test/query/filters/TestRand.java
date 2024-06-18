package dev.morphia.test.query.filters;

import dev.morphia.query.FindOptions;

import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.ComparisonExpressions.lt;
import static dev.morphia.aggregation.expressions.Miscellaneous.rand;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.expr;

public class TestRand extends FilterTest {

    /**
     * test data: dev/morphia/test/query/filters/rand/example1
     */
    @Test(testName = "Select Random Items From a Collection")
    public void testExample1() {
        QueryTestOptions options = new QueryTestOptions().orderMatters(false).removeIds(true).skipDataCheck(true)
                .findOptions(new FindOptions().projection().include("name", "registered"));
        testQuery(options, (query) -> query.filter(eq("district", 3), expr(lt(0.5, rand()))));
    }
}
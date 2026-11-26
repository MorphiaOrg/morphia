package dev.morphia.test.query.filters;

import java.util.List;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.all;
import static dev.morphia.query.filters.Filters.and;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.exists;
import static dev.morphia.query.filters.Filters.gt;
import static dev.morphia.query.filters.Filters.in;
import static dev.morphia.query.filters.Filters.lt;
import static dev.morphia.query.filters.Filters.ne;
import static dev.morphia.query.filters.Filters.nin;
import static dev.morphia.query.filters.Filters.or;

public class TestAnd extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/and/andQueriesWithMultipleExpressionsSpecifyingTheSameField
     */
    @Test
    @DisplayName("``AND`` Queries With Multiple Expressions Specifying the Same Field")
    public void testAndQueriesWithMultipleExpressionsSpecifyingTheSameField() {
        testQuery((query) -> query.filter(and(ne("price", 1.99), exists("price"))));
    }

    /**
     * test data: dev/morphia/test/query/filters/and/andQueriesWithMultipleExpressionsSpecifyingTheSameOperator
     */
    @Test
    @DisplayName("``AND`` Queries With Multiple Expressions Specifying the Same Operator")
    public void testAndQueriesWithMultipleExpressionsSpecifyingTheSameOperator() {
        testQuery((query) -> query.filter(and(or(lt("qty", 10), gt("qty", 50)), or(eq("sale", true), lt("price", 5)))));
    }

    @Test
    @DisplayName("Combine Conditions on One Field")
    public void testCombineConditionsOnOneField() {
        testQuery((query) -> query.filter(ne("price", 1.99), exists("price")));
    }

    @Test
    @DisplayName("Exclude Values with $nin")
    public void testExcludeValuesWithNin() {
        testQuery((query) -> query.filter(nin("status", List.of("closed", "archived"))));
    }

    @Test
    @DisplayName("Match Either Value with $in")
    public void testMatchEitherValueWithIn() {
        testQuery((query) -> query.filter(in("status", List.of("new", "processing"))));
    }

    @Test
    @DisplayName("Require Both Values with $all")
    public void testRequireBothValuesWithAll() {
        testQuery((query) -> query.filter(all("status", List.of("new", "processing"))));
    }
}

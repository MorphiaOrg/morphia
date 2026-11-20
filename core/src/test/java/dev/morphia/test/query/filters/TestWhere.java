package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.where;

public class TestWhere extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/where/whereQueryPredicateOperator
     */
    @Test
    @DisplayName("main")
    public void testWhereQueryPredicateOperator() {
        testQuery(new ActionTestOptions().skipActionCheck(true), (query) -> query.filter(where("""
                function() {
                   return (hex_md5(this.name) == "9b53e667f30cd329dca1ec9e6a83e994")
                }""")));
    }
}
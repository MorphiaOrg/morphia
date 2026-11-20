package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.ne;
import static dev.morphia.query.updates.UpdateOperators.set;

public class TestNe extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/ne/matchDocumentFieldsThatAreNotEqual
     */
    @Test
    @DisplayName("Match Document Fields That Are Not Equal")
    public void testMatchDocumentFieldsThatAreNotEqual() {
        testQuery(new ActionTestOptions().removeIds(true), (query) -> query.filter(ne("quantity", 20)));
    }

    /**
     * test data: dev/morphia/test/query/filters/ne/updateBasedOnNotEqualEmbeddedDocumentFields
     */
    @Test
    @DisplayName("Update Based on Not Equal Embedded Document Fields")
    public void testUpdateBasedOnNotEqualEmbeddedDocumentFields() {
        testUpdate(new ActionTestOptions().removeIds(true), (query) -> query.filter(ne("carrier.fee", 1)),
                set("price", 9.99));
    }

    /**
     * test data: dev/morphia/test/query/filters/ne/arrays
     * 
     */
    @Test
    @DisplayName("Arrays")
    public void testArrays() {
        testQuery(new ActionTestOptions().removeIds(true),
                (query) -> query.filter(ne("type", new String[] { "hardware", "fasteners" })));
    }
}
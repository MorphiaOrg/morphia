package dev.morphia.test.query.updates;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.and;
import static dev.morphia.query.updates.UpdateOperators.or;
import static dev.morphia.query.updates.UpdateOperators.xor;

public class TestBit extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/bit/bitwiseAnd
     */
    @Test
    @DisplayName("Bitwise AND")
    public void testBitwiseAnd() {
        testUpdate((query) -> query.filter(eq("_id", 1)), and("expdata", 10));
    }

    /**
     * test data: dev/morphia/test/query/updates/bit/bitwiseOr
     */
    @Test
    @DisplayName("Bitwise OR")
    public void testBitwiseOr() {
        testUpdate((query) -> query.filter(eq("_id", 2)), or("expdata", 5));
    }

    /**
     * test data: dev/morphia/test/query/updates/bit/bitwiseXor
     */
    @Test
    @DisplayName("Bitwise XOR")
    public void testBitwiseXor() {
        testUpdate((query) -> query.filter(eq("_id", 3)), xor("expdata", 5));
    }
}
package dev.morphia.test.query.filters;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.mod;

public class TestMod extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/mod/useModToSelectDocuments
     */
    @Test
    @DisplayName("Use ``$mod`` to Select Documents")
    public void testUseModToSelectDocuments() {
        testQuery((query) -> query.filter(mod("qty", 4, 0)));
    }

    /**
     * test data: dev/morphia/test/query/filters/mod/floatingPointArguments
     */
    @Test
    @DisplayName("Floating Point Arguments")
    public void testFloatingPointArguments() {
        testQuery((query) -> query.filter(mod("qty", 4.0, 0.0)));
    }

}
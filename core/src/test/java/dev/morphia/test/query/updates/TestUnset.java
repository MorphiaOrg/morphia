package dev.morphia.test.query.updates;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.unset;

public class TestUnset extends TemplatedTestBase {

    @Test
    @DisplayName("Remove Fields from a Document")
    public void testRemoveFieldsFromADocument() {
        testUpdate((query) -> query.filter(eq("title", "The Dark Knight")),
                unset("label"),
                unset("status"));
    }
}

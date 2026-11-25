package dev.morphia.test.query.updates;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.ne;
import static dev.morphia.query.updates.UpdateOperators.rename;

public class TestRename extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/rename/renameAField
     */
    @Test
    @DisplayName("Rename a Field")
    public void testRenameAField() {
        testUpdate((query) -> query.filter(ne("nmae", null)),
                rename("nmae", "name"));
    }

    /**
     * test data: dev/morphia/test/query/updates/rename/renameAFieldInAnEmbeddedDocument
     */
    @Test
    @DisplayName("Rename a Field in an Embedded Document")
    public void testRenameAFieldInAnEmbeddedDocument() {
        testUpdate((query) -> query.filter(eq("_id", 1)), rename("name.first", "name.fname"));
    }

    @Test
    @DisplayName("Rename a Field That Does Not Exist")
    public void testRenameAFieldThatDoesNotExist() {
        testUpdate((query) -> query.filter(eq("_id", 1)),
                rename("wife", "spouse"));
    }
}

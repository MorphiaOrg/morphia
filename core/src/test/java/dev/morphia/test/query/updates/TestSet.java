package dev.morphia.test.query.updates;

import java.util.List;

import dev.morphia.query.filters.Filters;
import dev.morphia.test.TemplatedTestBase;

import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.updates.UpdateOperators.set;

public class TestSet extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/updates/set/setTopLevelFields
     */
    @Test
    @DisplayName("Set Top-Level Fields")
    public void testSetTopLevelFields() {
        testUpdate((query) -> query.filter(Filters.eq("_id", 100)),
                set("quantity", 500), set("details", new Document("model", "2600").append("make", "Fashionaires")),
                set("tags", List.of("coats", "outerwear", "clothing")));
    }

    /**
     * test data: dev/morphia/test/query/updates/set/setFieldsInEmbeddedDocuments
     */
    @Test
    @DisplayName("Set Fields in Embedded Documents")
    public void testSetFieldsInEmbeddedDocuments() {
        testUpdate((query) -> query.filter(Filters.eq("_id", 100)), set("details.make", "Kustom Kidz"));
    }

    /**
     * test data: dev/morphia/test/query/updates/set/setElementsInArrays
     */
    @Test
    @DisplayName("Set Elements in Arrays")
    public void testSetElementsInArrays() {
        testUpdate((query) -> query.filter(Filters.eq("_id", 100)), set("tags.1", "rain gear"),
                set("ratings.0.rating", 2));
    }
}
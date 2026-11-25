package dev.morphia.test.query.filters;

import java.util.List;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;

public class TestEq extends TemplatedTestBase {
    /**
     * test data: dev/morphia/test/query/filters/eq/equalsASpecifiedValue
     * 
     */
    @Test
    @DisplayName("Equals a Specified Value")
    public void testEqualsASpecifiedValue() {
        testQuery((query) -> query.filter(eq("qty", 20)));
    }

    /**
     * test data: dev/morphia/test/query/filters/eq/fieldInEmbeddedDocumentEqualsAValue
     * 
     */
    @Test
    @DisplayName("Field in Embedded Document Equals a Value")
    public void testFieldInEmbeddedDocumentEqualsAValue() {
        testQuery((query) -> query.filter(eq("item.name", "ab")));
    }

    /**
     * test data: dev/morphia/test/query/filters/eq/arrayElementEqualsAValue
     * 
     */
    @Test
    @DisplayName("Array Element Equals a Value")
    public void testArrayElementEqualsAValue() {
        testQuery((query) -> query.filter(eq("tags", "B")));
    }

    /**
     * test data: dev/morphia/test/query/filters/eq/equalsAnArrayValue
     * 
     */
    @Test
    @DisplayName("Equals an Array Value")
    public void testEqualsAnArrayValue() {
        testQuery((query) -> query.filter(eq("tags", List.of("A", "B"))

        ));
    }

    @Test
    @DisplayName("Match a Quantity Exactly")
    public void testMatchAQuantityExactly() {
        testQuery((query) -> query.filter(eq("qty", 20)));
    }

    @Test
    @DisplayName("Match an Array Exactly")
    public void testMatchAnArrayExactly() {
        testQuery((query) -> query.filter(eq("tags", List.of("A", "B"))));
    }

    @Test
    @DisplayName("Match an Embedded Field with Dot Notation")
    public void testMatchAnEmbeddedFieldWithDotNotation() {
        testQuery((query) -> query.filter(eq("item.name", "ab")));
    }

    @Test
    @DisplayName("Match One Array Element")
    public void testMatchOneArrayElement() {
        testQuery((query) -> query.filter(eq("tags", "B")));
    }
}

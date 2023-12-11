package dev.morphia.test.util;

import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ComparanatorTest {
    @Test
    public void testObjects() {

        Comparanator.of(null, 42, 42, false).compare();

        assertThrows(AssertionError.class, () -> {
            Comparanator.of(null, 42, 12, false).compare();
        });
    }

    @Test
    public void testLists() {
        Comparanator.of(null, List.of("a", "b", "c"), List.of("a", "b", "c"), true).compare();
        assertThrows(AssertionError.class, () -> {
            Comparanator.of(null, List.of("a", "b", "c"), List.of("a", "c", "b"), true).compare();
        });

        Comparanator.of(null, List.of("a", "b", "c"), List.of("a", "c", "b"), false).compare();
        assertThrows(AssertionError.class, () -> {
            Comparanator.of(null, List.of("a", "b", "c"), List.of("a", "c"), true).compare();
        });
    }

    @Test
    public void testMaps() {
        Comparanator.of(null, Map.of("one", 1, "two", 2), Map.of("one", 1, "two", 2), true)
                .compare();
        assertThrows(AssertionError.class, () -> {
            Comparanator.of(null,
                    Map.of("one", 1, "two", 2, "three", 3),
                    Map.of("one", 1, "two", 2), true)
                    .compare();
        });
        assertThrows(AssertionError.class, () -> {
            Comparanator.of(null,
                    Map.of("one", 1, "two", 2, "four", 5),
                    Map.of("one", 1, "two", 2, "four", 4), true)
                    .compare();
        });
        Comparanator.of(null,
                Map.of("one", 1, "two", 2, "four", 4),
                Map.of("one", 1, "four", 4, "two", 2), true)
                .compare();
    }

}
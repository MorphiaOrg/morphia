package dev.morphia.test.query.filters;

import java.util.regex.Pattern;

import dev.morphia.test.TemplatedTestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.regex;

public class TestRegex extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/regex/extendRegexOptionsToMatchCharactersOutsideOfAscii
     */
    @Test
    @DisplayName("Extend Regex Options to Match Characters Outside of ASCII")
    public void testExtendRegexOptionsToMatchCharactersOutsideOfAscii() {
        testQuery((query) -> query.filter(regex("artist", Pattern.compile("\\byster"))));
    }
}
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

    @Test
    @DisplayName("Ignore White Spaces in Pattern")
    public void testIgnoreWhiteSpacesInPattern() {
        testQuery((query) -> query.filter(regex("sku", "abc #category code\n123 #item number").extended()));
    }

    @Test
    @DisplayName("Match Lines That Start with S")
    public void testMatchLinesThatStartWithS() {
        testQuery((query) -> query.filter(regex("description", "^S").multiline()));
    }

    @Test
    @DisplayName("Match S Anywhere")
    public void testMatchSAnywhere() {
        testQuery((query) -> query.filter(regex("description", "S")));
    }

    @Test
    @DisplayName("Perform Case-Insensitive Regular Expression Match")
    public void testPerformCaseInsensitiveRegularExpressionMatch() {
        testQuery((query) -> query.filter(regex("sku", "^ABC").caseInsensitive()));
    }

    @Test
    @DisplayName("Use a Regular Expression to Match Case in Strings")
    public void testUseARegularExpressionToMatchCaseInStrings() {
        testQuery((query) -> query.filter(regex("sku", "(?i)a(?-i)bc")));
    }

    @Test
    @DisplayName("Use the . Dot Character to Match New Line")
    public void testUseTheDotCharacterToMatchNewLine() {
        testQuery((query) -> query.filter(regex("description", "m.*line").caseInsensitive().special()));
    }
}

package dev.morphia.test.query.filters;

import java.util.regex.Pattern;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.regex;

public class TestRegex extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/regex/performALikeMatch
     */
    @Test
    @DisplayName("Perform a ``LIKE`` Match")
    public void testPerformALikeMatch() {
        testQuery((query) -> query.filter(regex("sku", Pattern.compile("789$"))));
    }

    /**
     * test data: dev/morphia/test/query/filters/regex/performCaseInsensitiveRegularExpressionMatch
     */
    @Test
    @DisplayName("Perform Case-Insensitive Regular Expression Match")
    public void testPerformCaseInsensitiveRegularExpressionMatch() {
        testQuery((query) -> query.filter(regex("sku", Pattern.compile("^ABC")).caseInsensitive()));
    }

    /**
     * test data: dev/morphia/test/query/filters/regex/multilineMatchForLinesStartingWithSpecifiedPattern
     */
    @Test
    @DisplayName("Multiline Match for Lines Starting with Specified Pattern")
    public void testMultilineMatchForLinesStartingWithSpecifiedPattern() {
        testQuery((query) -> query.filter(regex("description", Pattern.compile("^S")).multiline()));
    }

    /**
     * test data: dev/morphia/test/query/filters/regex/useTheDotCharacterToMatchNewLine
     */
    @Test
    @DisplayName("Use the ``.`` Dot Character to Match New Line")
    public void testUseTheDotCharacterToMatchNewLine() {
        testQuery(
                (query) -> query.filter(regex("description", Pattern.compile("m.*line")).special().caseInsensitive()));
    }

    /**
     * test data: dev/morphia/test/query/filters/regex/ignoreWhiteSpacesInPattern
     */
    @Test
    @DisplayName("Ignore White Spaces in Pattern")
    public void testIgnoreWhiteSpacesInPattern() {
        testQuery(new ActionTestOptions().serverVersion("7.0.0").skipActionCheck(true), (query) -> query
                .filter(regex("sku", Pattern.compile("abc #category code\n123 #item number")).extended()));
    }

    /**
     * test data: dev/morphia/test/query/filters/regex/useARegularExpressionToMatchCaseInStrings
     */
    @Test
    @DisplayName("Use a Regular Expression to Match Case in Strings")
    public void testUseARegularExpressionToMatchCaseInStrings() {
        testQuery((query) -> query.filter(regex("sku", Pattern.compile("(?i)a(?-i)bc"))));
    }

    /**
     * test data: dev/morphia/test/query/filters/regex/extendRegexOptionsToMatchCharactersOutsideOfAscii
     */
    @Test
    @DisplayName("Extend Regex Options to Match Characters Outside of ASCII")
    public void testExtendRegexOptionsToMatchCharactersOutsideOfAscii() {
        testQuery((query) -> query.filter(regex("artist", Pattern.compile("\\byster"))));
    }
}
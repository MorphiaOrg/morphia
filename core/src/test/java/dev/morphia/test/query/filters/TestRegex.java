package dev.morphia.test.query.filters;

import java.util.regex.Pattern;

import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.util.ActionTestOptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.regex;

public class TestRegex extends TemplatedTestBase {

    /**
     * test data: dev/morphia/test/query/filters/regex/example1
     */
    @Test
    @DisplayName("Perform a ``LIKE`` Match")
    public void testExample1() {
        testQuery((query) -> query.filter(regex("sku", Pattern.compile("789$"))));
    }

    /**
     * test data: dev/morphia/test/query/filters/regex/example2
     */
    @Test
    @DisplayName("Perform Case-Insensitive Regular Expression Match")
    public void testExample2() {
        testQuery((query) -> query.filter(regex("sku", Pattern.compile("^ABC")).caseInsensitive()));
    }

    /**
     * test data: dev/morphia/test/query/filters/regex/example3
     */
    @Test
    @DisplayName("Multiline Match for Lines Starting with Specified Pattern")
    public void testExample3() {
        testQuery((query) -> query.filter(regex("description", Pattern.compile("^S")).multiline()));
    }

    /**
     * test data: dev/morphia/test/query/filters/regex/example4
     */
    @Test
    @DisplayName("Use the ``.`` Dot Character to Match New Line")
    public void testExample4() {
        testQuery(
                (query) -> query.filter(regex("description", Pattern.compile("m.*line")).special().caseInsensitive()));
    }

    /**
     * test data: dev/morphia/test/query/filters/regex/example5
     */
    @Test
    @DisplayName("Ignore White Spaces in Pattern")
    public void testExample5() {
        testQuery(new ActionTestOptions().serverVersion("7.0.0").skipActionCheck(true), (query) -> query
                .filter(regex("sku", Pattern.compile("abc #category code\n123 #item number")).extended()));
    }

    /**
     * test data: dev/morphia/test/query/filters/regex/example6
     */
    @Test
    @DisplayName("Use a Regular Expression to Match Case in Strings")
    public void testExample6() {
        testQuery((query) -> query.filter(regex("sku", Pattern.compile("(?i)a(?-i)bc"))));
    }

    /**
     * test data: dev/morphia/test/query/filters/regex/example7
     */
    @Test
    @DisplayName("Extend Regex Options to Match Characters Outside of ASCII")
    public void testExample7() {
        testQuery((query) -> query.filter(regex("artist", Pattern.compile("\\byster"))));
    }
}
package dev.morphia.aggregation.experimental.expressions;

import org.junit.Test;

import java.util.List;
import java.util.regex.Pattern;

import static dev.morphia.aggregation.experimental.expressions.Expressions.value;
import static dev.morphia.aggregation.experimental.expressions.StringExpressions.concat;
import static dev.morphia.aggregation.experimental.expressions.StringExpressions.indexOfBytes;
import static dev.morphia.aggregation.experimental.expressions.StringExpressions.indexOfCP;
import static dev.morphia.aggregation.experimental.expressions.StringExpressions.ltrim;
import static dev.morphia.aggregation.experimental.expressions.StringExpressions.regexFind;
import static dev.morphia.aggregation.experimental.expressions.StringExpressions.regexFindAll;
import static dev.morphia.aggregation.experimental.expressions.StringExpressions.regexMatch;
import static dev.morphia.aggregation.experimental.expressions.StringExpressions.rtrim;
import static dev.morphia.aggregation.experimental.expressions.StringExpressions.split;
import static dev.morphia.aggregation.experimental.expressions.StringExpressions.strLenBytes;
import static dev.morphia.aggregation.experimental.expressions.StringExpressions.strLenCP;
import static dev.morphia.aggregation.experimental.expressions.StringExpressions.strcasecmp;
import static dev.morphia.aggregation.experimental.expressions.StringExpressions.substr;
import static dev.morphia.aggregation.experimental.expressions.StringExpressions.substrBytes;
import static dev.morphia.aggregation.experimental.expressions.StringExpressions.substrCP;
import static dev.morphia.aggregation.experimental.expressions.StringExpressions.toLower;
import static dev.morphia.aggregation.experimental.expressions.StringExpressions.toUpper;
import static dev.morphia.aggregation.experimental.expressions.StringExpressions.trim;
import static org.bson.Document.parse;

public class StringExpressionsTest extends ExpressionsTest {

    @Test
    public void testConcat() {
        evaluate("{ $concat: [ 'item', ' - ', 'description' ] }", concat(value("item"), value(" - "), value("description")),
            "item - description");
    }

    @Test
    public void testIndexOfBytes() {
        evaluate("{ $indexOfBytes: [ 'item', 'foo' ] }", indexOfBytes(value("item"), value("foo")), -1);
        evaluate("{ $indexOfBytes: [ 'winter wonderland', 'winter' ] }",
            indexOfBytes(value("winter wonderland"), value("winter"))
                .start(4), -1);
        evaluate("{ $indexOfBytes: [ 'winter wonderland', 'winter' ] }",
            indexOfBytes(value("winter wonderland"), value("winter"))
                .end(5), -1);
    }

    @Test
    public void testIndexOfCP() {
        evaluate("{ $indexOfCP: [ 'item', 'foo' ] }", indexOfCP(value("item"), value("foo")), -1);
        evaluate("{ $indexOfCP: [ 'winter wonderland', 'winter' ] }",
            indexOfCP(value("winter wonderland"), value("winter"))
                .start(4), -1);
        evaluate("{ $indexOfCP: [ 'winter wonderland', 'winter' ] }",
            indexOfCP(value("winter wonderland"), value("winter"))
                .end(5), -1);
    }

    @Test
    public void testLtrim() {
        evaluate("{ $ltrim: { input: '    winter wonderland' } }", ltrim(value("    winter wonderland")),
            "winter wonderland");
        evaluate("{ $ltrim: { input: 'winter wonderland' } }", ltrim(value("winter wonderland"))
                                                                   .chars(value("winter")),
            " wonderland");
    }

    @Test
    public void testRegexFind() {
        evaluate("{ $regexFind: { input: 'winter wonderland', regex: /inter/ } }", regexFind(value("winter wonderland"))
                                                                                       .regex("inter"),
            parse("{match: 'inter', idx:1, captures:[]}"));
        evaluate("{ $regexFind: { input: 'winter wonderland', regex: /inter/ } }", regexFind(value("winter wonderland"))
                                                                                       .regex(Pattern.compile("inter")),
            parse("{match: 'inter', idx:1, captures:[]}"));
        evaluate("{ $regexFind: { input: 'winter wonderland', regex: /splinter/ } }", regexFind(value("winter wonderland"))
                                                                                          .regex("splinter"), null);
    }

    @Test
    public void testRegexFindAll() {
        evaluate("{ $regexFindAll: { input: 'winter wonderland', regex: /inter/ } }",
            regexFindAll(value("winter wonderland")).regex("inter"),
            List.of(parse("{match: 'inter', idx:1, captures:[]}")));
        evaluate("{ $regexFindAll: { input: 'winter wonderland', regex: /inter/ } }",
            regexFindAll(value("winter wonderland")).regex(Pattern.compile("inter")),
            List.of(parse("{match: 'inter', idx:1, captures:[]}")));
        evaluate("{ $regexFindAll: { input: 'winter wonderland', regex: /splinter/ } }",
            regexFindAll(value("winter wonderland")).regex("splinter"), List.of());
    }

    @Test
    public void testRegexMatch() {
        evaluate("{ $regexMatch: { input: 'winter wonderland', regex: /inter/ } }",
            regexMatch(value("winter wonderland")).regex("inter"), true);
        evaluate("{ $regexMatch: { input: 'winter wonderland', regex: /inter/ } }",
            regexMatch(value("winter wonderland")).regex(Pattern.compile("inter")), true);
        evaluate("{ $regexMatch: { input: 'winter wonderland', regex: /splinter/ } }",
            regexMatch(value("winter wonderland")).regex("splinter"), false);
    }

    @Test
    public void testRtrim() {
        evaluate("{ $rtrim: { input: 'winter wonderland    ' } }", rtrim(value("winter wonderland    ")),
            "winter wonderland");
        evaluate("{ $rtrim: { input: 'winter wonderland' } }", rtrim(value("winter wonderland"))
                                                                   .chars(value("land")),
            "winter wonder");
    }

    @Test
    public void testSplit() {
        evaluate("{ $split: [ 'June-15-2013', '-' ] }", split(value("June-15-2013"), value("-")),
            List.of("June", "15", "2013"));
    }

    @Test
    public void testStrLenBytes() {
        evaluate("{ $strLenBytes: 'abcde' }", strLenBytes(value("abcde")), 5);
    }

    @Test
    public void testStrLenCP() {
        evaluate("{ $strLenCP: 'abcde' }", strLenCP(value("abcde")), 5);
    }

    @Test
    public void testStrcasecmp() {
        evaluate("{ $strcasecmp: [ 'abcde', 'ABCDEF' ] }", strcasecmp(value("abcde"), value("ABCDEF")), -1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSubstr() {
        evaluate("{ $substr: [ 'winter wonderland', 3, 5 ] }", substr(value("winter wonderland"), 3, 5),
            "ter w");
    }

    @Test
    public void testSubstrBytes() {
        evaluate("{ $substrBytes: [ 'winter wonderland', 3, 5 ] }", substrBytes(value("winter wonderland"), 3, 5),
            "ter w");
    }

    @Test
    public void testSubstrCP() {
        evaluate("{ $substrCP: [ 'winter wonderland', 3, 5 ] }", substrCP(value("winter wonderland"), 3, 5),
            "ter w");
    }

    @Test
    public void testToLower() {
        evaluate("{ $toLower: 'HELLO' }", toLower(value("HELLO")), "hello");
    }

    @Test
    public void testToString() {
        evaluate("{ $toString: 12345 }", StringExpressions.toString(value(12345)), "12345");
    }

    @Test
    public void testToUpper() {
        evaluate("{ $toUpper: 'hello' }", toUpper(value("hello")), "HELLO");
    }

    @Test
    public void testTrim() {
        evaluate("{ $trim: { input: '   books   ' } }", trim(value("   books   ")), "books");
        evaluate("{ $trim: { input: '===books===' } }", trim(value("===books===")).chars(value("===")), "books");
    }
}

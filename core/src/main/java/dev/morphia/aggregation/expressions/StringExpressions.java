package dev.morphia.aggregation.expressions;

import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.IndexExpression;
import dev.morphia.aggregation.expressions.impls.RegexExpression;
import dev.morphia.aggregation.expressions.impls.ReplaceExpression;
import dev.morphia.aggregation.expressions.impls.TrimExpression;
import dev.morphia.aggregation.expressions.impls.ValueExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.asList;

/**
 * Defines helper methods for the string expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#string-expression-operators String Expressions
 * @since 2.0
 */
public final class StringExpressions {
    private static final Logger LOG = LoggerFactory.getLogger(StringExpressions.class);

    private StringExpressions() {
    }

    /**
     * Concatenates any number of strings.
     *
     * @param first      the first array expression
     * @param additional additional expressions
     * @return the new expression
     * @aggregation.expression $concat
     */
    public static Expression concat(Expression first, Expression... additional) {
        return new Expression("$concat", Expressions.toList(first, additional));
    }

    /**
     * Searches a string for an occurence of a substring and returns the UTF-8 byte index of the first occurence. If the substring is not
     * found, returns -1.
     *
     * @param string    the string to search
     * @param substring the target string
     * @return the new expression
     * @aggregation.expression $indexOfBytes
     */
    public static IndexExpression indexOfBytes(Expression string, Expression substring) {
        return new IndexExpression("$indexOfBytes", string, substring);
    }

    /**
     * Searches a string for an occurence of a substring and returns the UTF-8 code point index of the first occurence. If the substring is
     * not found, returns -1
     *
     * @param string    the string to search
     * @param substring the target string
     * @return the new expression
     * @aggregation.expression $indexOfCP
     */
    public static IndexExpression indexOfCP(Expression string, Expression substring) {
        return new IndexExpression("$indexOfCP", string, substring);
    }

    /**
     * Removes whitespace or the specified characters from the beginning of a string.
     *
     * @param input The string to trim. The argument can be any valid expression that resolves to a string.
     * @return the new expression
     * @aggregation.expression $ltrim
     */
    public static TrimExpression ltrim(Expression input) {
        return new TrimExpression("$ltrim", input);
    }

    /**
     * Applies a regular expression (regex) to a string and returns information on the first matched substring.
     *
     * @param input the string to evaluate
     * @return the new expression
     * @aggregation.expression $regexFind
     */
    public static RegexExpression regexFind(Expression input) {
        return new RegexExpression("$regexFind", input);
    }

    /**
     * Applies a regular expression (regex) to a string and returns information on the all matched substrings.
     *
     * @param input the string to evaluate
     * @return the new expression
     * @aggregation.expression $regexFindAll
     */
    public static RegexExpression regexFindAll(Expression input) {
        return new RegexExpression("$regexFindAll", input);
    }

    /**
     * Applies a regular expression (regex) to a string and returns a boolean that indicates if a match is found or not.
     *
     * @param input the string to process
     * @return the new expression
     * @aggregation.expression $regexMatch
     */
    public static RegexExpression regexMatch(Expression input) {
        return new RegexExpression("$regexMatch", input);
    }

    /**
     * Replaces all instances of a search string in an input string with a replacement string.
     *
     * @param input       the input value/source
     * @param find        the search expression
     * @param replacement the replacement value
     * @return the new expression
     * @aggregation.expression $replaceAll
     * @since 2.1
     */
    public static Expression replaceAll(Expression input, Expression find, Expression replacement) {
        return new ReplaceExpression("$replaceAll", input, find, replacement);
    }

    /**
     * Replaces the first instance of a search string in an input string with a replacement string.
     *
     * @param input       the input value/source
     * @param find        the search expression
     * @param replacement the replacement value
     * @return the new expression
     * @aggregation.expression $replaceOne
     * @since 2.1
     */
    public static Expression replaceOne(Expression input, Expression find, Expression replacement) {
        return new ReplaceExpression("$replaceOne", input, find, replacement);
    }

    /**
     * Removes whitespace or the specified characters from the end of a string.
     *
     * @param input The string to trim. The argument can be any valid expression that resolves to a string.
     * @return the new expression
     * @aggregation.expression $rtrim
     */
    public static TrimExpression rtrim(Expression input) {
        return new TrimExpression("$rtrim", input);
    }

    /**
     * Splits a string into substrings based on a delimiter. Returns an array of substrings. If the delimiter is not found within the
     * string,
     * returns an array containing the original string.
     *
     * @param input     The string to split. The argument can be any valid expression that resolves to a string.
     * @param delimiter The delimiter to use when splitting the string expression. delimiter can be any valid expression as long as it
     *                  resolves to a string.
     * @return the new expression
     * @aggregation.expression $split
     */
    public static Expression split(Expression input, Expression delimiter) {
        return new Expression("$split", asList(input, delimiter));
    }

    /**
     * Returns the number of UTF-8 encoded bytes in a string.
     *
     * @param input the string to process
     * @return the new expression
     * @aggregation.expression $strLenBytes
     */
    public static Expression strLenBytes(Expression input) {
        return new Expression("$strLenBytes", input);
    }

    /**
     * Returns the number of UTF-8 code points in a string.
     *
     * @param input the string to process
     * @return the new expression
     * @aggregation.expression $strLenCP
     */
    public static Expression strLenCP(Expression input) {
        return new Expression("$strLenCP", input);
    }

    /**
     * Performs case-insensitive string comparison and returns: 0 if two strings are equivalent, 1 if the first string is greater than the
     * second, and -1 if the first string is less than the second.
     *
     * @param first  the first string to compare
     * @param second the first string to second
     * @return the new expression
     * @aggregation.expression $strcasecmp
     */
    public static Expression strcasecmp(Expression first, Expression second) {
        return new Expression("$strcasecmp", asList(first, second));
    }

    /**
     * Deprecated. Use $substrBytes or $substrCP.
     * <p>
     * *note*:  Included for completeness and discoverability.
     *
     * @param input  the string to process
     * @param start  the starting position
     * @param length the number of characters
     * @return the new expression
     * @aggregation.expression $substr
     * @deprecated Deprecated since version 3.4: $substr is now an alias for {@link #substrBytes(Expression, int, int)}
     */
    @Deprecated
    @SuppressWarnings("unused")
    public static Expression substr(Expression input, int start, int length) {
        throw new UnsupportedOperationException("Use $substrBytes or $substrCP.");
    }

    /**
     * Returns the substring of a string. Starts with the character at the specified UTF-8 byte index (zero-based) in the string and
     * continues for the specified number of bytes.
     *
     * @param input  the string to process
     * @param start  Indicates the starting point of the substring
     * @param length the byte count to include.  Can not result in an ending index that is in the middle of a UTF-8 character.
     * @return the new expression
     * @aggregation.expression $substrBytes
     */
    public static Expression substrBytes(Expression input, int start, int length) {
        return new Expression("$substrBytes", asList(input, new ValueExpression(start), new ValueExpression(length)));
    }

    /**
     * Returns the substring of a string. Starts with the character at the specified UTF-8 code point (CP) index (zero-based) in the string
     * and continues for the number of code points specified.
     *
     * @param input  the string to process
     * @param start  Indicates the starting point of the substring
     * @param length the code points to include.
     * @return the new expression
     * @aggregation.expression $substrCP
     */
    public static Expression substrCP(Expression input, int start, int length) {
        return new Expression("$substrCP", asList(input, new ValueExpression(start), new ValueExpression(length)));
    }

    /**
     * Converts a string to lowercase. Accepts a single argument expression.
     *
     * @param input the string to process
     * @return the new expression
     * @aggregation.expression $toLower
     */
    public static Expression toLower(Expression input) {
        return new Expression("$toLower", input);
    }

    /**
     * Converts value to a string.
     *
     * @param input the value to process
     * @return the new expression
     * @aggregation.expression $toString
     */
    public static Expression toString(Expression input) {
        return new Expression("$toString", input);
    }

    /**
     * Converts a string to uppercase. Accepts a single argument expression.
     *
     * @param input the string to process
     * @return the new expression
     * @aggregation.expression $toUpper
     */
    public static Expression toUpper(Expression input) {
        return new Expression("$toUpper", input);
    }

    /**
     * Removes whitespace or the specified characters from the beginning and end of a string.
     *
     * @param input the string to process
     * @return the new expression
     * @aggregation.expression $trim
     */
    public static TrimExpression trim(Expression input) {
        return new TrimExpression("$trim", input);
    }

}

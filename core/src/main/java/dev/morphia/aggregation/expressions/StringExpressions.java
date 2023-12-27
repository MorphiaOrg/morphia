package dev.morphia.aggregation.expressions;

import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.IndexExpression;
import dev.morphia.aggregation.expressions.impls.RegexExpression;
import dev.morphia.aggregation.expressions.impls.ReplaceExpression;
import dev.morphia.aggregation.expressions.impls.TrimExpression;
import dev.morphia.aggregation.expressions.impls.ValueExpression;

import static dev.morphia.aggregation.expressions.Expressions.wrap;
import static java.util.Arrays.asList;

/**
 * Defines helper methods for the string expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#string-expression-operators String Expressions
 * @since 2.0
 */
public final class StringExpressions {
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
    public static Expression concat(Object first, Object... additional) {
        return new Expression("$concat", wrap(first, additional));
    }

    /**
     * Searches a string for an occurrence of a substring and returns the UTF-8 byte index of the first occurrence. If the substring is not
     * found, returns -1.
     *
     * @param string    the string to search
     * @param substring the target string
     * @return the new expression
     * @aggregation.expression $indexOfBytes
     */
    public static IndexExpression indexOfBytes(Object string, Object substring) {
        return new IndexExpression("$indexOfBytes", wrap(string), wrap(substring));
    }

    /**
     * Searches a string for an occurrence of a substring and returns the UTF-8 code point index of the first occurrence. If the
     * substring is not found, returns -1
     *
     * @param string    the string to search
     * @param substring the target string
     * @return the new expression
     * @aggregation.expression $indexOfCP
     */
    public static IndexExpression indexOfCP(Object string, Object substring) {
        return new IndexExpression("$indexOfCP", wrap(string), wrap(substring));
    }

    /**
     * Removes whitespace or the specified characters from the beginning of a string.
     *
     * @param input The string to trim. The argument can be any valid expression that resolves to a string.
     * @return the new expression
     * @aggregation.expression $ltrim
     */
    public static TrimExpression ltrim(Object input) {
        return new TrimExpression("$ltrim", wrap(input));
    }

    /**
     * Applies a regular expression (regex) to a string and returns information on the first matched substring.
     *
     * @param input the string to evaluate
     * @return the new expression
     * @aggregation.expression $regexFind
     */
    public static RegexExpression regexFind(Object input) {
        return new RegexExpression("$regexFind", wrap(input));
    }

    /**
     * Applies a regular expression (regex) to a string and returns information on the all matched substrings.
     *
     * @param input the string to evaluate
     * @return the new expression
     * @aggregation.expression $regexFindAll
     */
    public static RegexExpression regexFindAll(Object input) {
        return new RegexExpression("$regexFindAll", wrap(input));
    }

    /**
     * Applies a regular expression (regex) to a string and returns a boolean that indicates if a match is found or not.
     *
     * @param input the string to process
     * @return the new expression
     * @aggregation.expression $regexMatch
     */
    public static RegexExpression regexMatch(Object input) {
        return new RegexExpression("$regexMatch", wrap(input));
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
    public static Expression replaceAll(Object input, Object find, Object replacement) {
        return new ReplaceExpression("$replaceAll", wrap(input), wrap(find), wrap(replacement));
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
    public static Expression replaceOne(Object input, Object find, Object replacement) {
        return new ReplaceExpression("$replaceOne", wrap(input), wrap(find), wrap(replacement));
    }

    /**
     * Removes whitespace or the specified characters from the end of a string.
     *
     * @param input The string to trim. The argument can be any valid expression that resolves to a string.
     * @return the new expression
     * @aggregation.expression $rtrim
     */
    public static TrimExpression rtrim(Object input) {
        return new TrimExpression("$rtrim", wrap(input));
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
    public static Expression split(Object input, Object delimiter) {
        return new Expression("$split", wrap(asList(input, delimiter)));
    }

    /**
     * Returns the number of UTF-8 encoded bytes in a string.
     *
     * @param input the string to process
     * @return the new expression
     * @aggregation.expression $strLenBytes
     */
    public static Expression strLenBytes(Object input) {
        return new Expression("$strLenBytes", wrap(input));
    }

    /**
     * Returns the number of UTF-8 code points in a string.
     *
     * @param input the string to process
     * @return the new expression
     * @aggregation.expression $strLenCP
     */
    public static Expression strLenCP(Object input) {
        return new Expression("$strLenCP", wrap(input));
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
    public static Expression strcasecmp(Object first, Object second) {
        return new Expression("$strcasecmp", wrap(asList(first, second)));
    }

    /**
     * Returns the substring of a string. Starts with the character at the specified UTF-8 byte index (zero-based) in the string and
     * continues for the specified number of bytes.
     *
     * @param input  the string to process
     * @param start  Indicates the starting point of the substring
     * @param length the byte count to include. Can not result in an ending index that is in the middle of a UTF-8 character.
     * @return the new expression
     * @aggregation.expression $substrBytes
     */
    public static Expression substrBytes(Object input, int start, int length) {
        return substrBytes(input, new ValueExpression(start), new ValueExpression(length));
    }

    /**
     * Returns the substring of a string. Starts with the character at the specified UTF-8 byte index (zero-based) in the string and
     * continues for the specified number of bytes.
     *
     * @param input  the string to process
     * @param start  Indicates the starting point of the substring
     * @param length the byte count to include. Can not result in an ending index that is in the middle of a UTF-8 character.
     * @return the new expression
     * @aggregation.expression $substrBytes
     */
    public static Expression substrBytes(Object input, Object start, Object length) {
        return new Expression("$substrBytes", wrap(asList(input, start, length)));
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
    public static Expression substrCP(Object input, Object start, Object length) {
        return new Expression("$substrCP", wrap(asList(input, start, length)));
    }

    /**
     * Converts a string to lowercase. Accepts a single argument expression.
     *
     * @param input the string to process
     * @return the new expression
     * @aggregation.expression $toLower
     */
    public static Expression toLower(Object input) {
        return new Expression("$toLower", wrap(input));
    }

    /**
     * Converts value to a string.
     *
     * @param input the value to process
     * @return the new expression
     * @aggregation.expression $toString
     */
    public static Expression toString(Object input) {
        return new Expression("$toString", wrap(input));
    }

    /**
     * Converts a string to uppercase. Accepts a single argument expression.
     *
     * @param input the string to process
     * @return the new expression
     * @aggregation.expression $toUpper
     */
    public static Expression toUpper(Object input) {
        return new Expression("$toUpper", wrap(input));
    }

    /**
     * Removes whitespace or the specified characters from the beginning and end of a string.
     *
     * @param input the string to process
     * @return the new expression
     * @aggregation.expression $trim
     */
    public static TrimExpression trim(Object input) {
        return new TrimExpression("$trim", wrap(input));
    }

}

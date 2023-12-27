package dev.morphia.aggregation.expressions;

import java.util.List;

import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.MathExpression;
import dev.morphia.aggregation.expressions.impls.MedianExpression;
import dev.morphia.aggregation.expressions.impls.PercentileExpression;

import static dev.morphia.aggregation.expressions.Expressions.wrap;
import static java.util.Arrays.asList;

/**
 * Defines helper methods for the math expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#arithmetic-expression-operators Arithmetic Expressions
 * @since 2.0
 */
public final class MathExpressions {
    private MathExpressions() {
    }

    /**
     * Returns the absolute value of a number.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $abs
     */
    public static Expression abs(Object value) {
        return new MathExpression("$abs", wrap(value));
    }

    /**
     * Adds numbers together or adds numbers and a date. If one of the arguments is a date, $add treats the other arguments as
     * milliseconds to add to the date.
     *
     * @param first      the first value to sum
     * @param additional any subsequent values to include in the sum
     * @return the new expression
     * @aggregation.expression $add
     */
    public static Expression add(Object first, Object... additional) {
        return new MathExpression("$add", wrap(first, additional));
    }

    /**
     * Returns the result of a bitwise and operation on an array of int or long values.
     *
     * @param first  the first value to use
     * @param second the second value to use
     * @return the new expression
     * @aggregation.expression $bitAnd
     * @since 3.0
     */
    public static Expression bitAnd(Object first, Object second) {
        return new MathExpression("$bitAnd", wrap(List.of(first, second)));
    }

    /**
     * Returns the result of a bitwise not operation on a single int or long value.
     *
     * @param value the value to use
     * @return the new expression
     * @aggregation.expression $bitNot
     * @since 3.0
     */
    public static Expression bitNot(Object value) {
        return new MathExpression("$bitNot", wrap(value));
    }

    /**
     * Returns the result of a bitwise or operation on an array of int or long values.
     *
     * @param first  the first value to use
     * @param second the second value to use
     * @return the new expression
     * @aggregation.expression $bitOr
     * @since 3.0
     */
    public static Expression bitOr(Object first, Object second) {
        return new MathExpression("$bitOr", wrap(List.of(first, second)));
    }

    /**
     * Returns the result of a bitwise xor operation on an array of int xor long values.
     *
     * @param first  the first value to use
     * @param second the second value to use
     * @return the new expression
     * @aggregation.expression $bitXor
     * @since 3.0
     */
    public static Expression bitXor(Object first, Object second) {
        return new MathExpression("$bitXor", wrap(List.of(first, second)));
    }

    /**
     * Returns the smallest integer greater than or equal to the specified number.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $ceil
     */
    public static Expression ceil(Object value) {
        return new MathExpression("$ceil", wrap(value));
    }

    /**
     * Returns the result of dividing the first number by the second. Accepts two argument expressions.
     *
     * @param numerator the numerator
     * @param divisor   the divisor
     * @return the new expression
     * @aggregation.expression $divide
     */
    public static Expression divide(Object numerator, Object divisor) {
        return new MathExpression("$divide", wrap(List.of(numerator, divisor)));
    }

    /**
     * Raises e to the specified exponent.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $exp
     */
    public static Expression exp(Object value) {
        return new MathExpression("$exp", wrap(value));
    }

    /**
     * Returns the largest integer less than or equal to the specified number.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $floor
     */
    public static Expression floor(Object value) {
        return new MathExpression("$floor", wrap(value));
    }

    /**
     * Calculates the natural log of a number.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $ln
     */
    public static Expression ln(Object value) {
        return new MathExpression("$ln", wrap(value));
    }

    /**
     * Calculates the log of a number in the specified base.
     *
     * @param number the number to log
     * @param base   the base to use
     * @return the new expression
     * @aggregation.expression $log
     */
    public static Expression log(Object number, Object base) {
        return new MathExpression("$log", wrap(List.of(number, base)));
    }

    /**
     * Calculates the log base 10 of a number.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $log10
     */
    public static Expression log10(Object value) {
        return new MathExpression("$log10", wrap(value));
    }

    /**
     * Returns an approximation of the median, the 50th percentile, as a scalar value.
     *
     * @param input the input
     * @return the new expression
     * @aggregation.expression $median
     * @since 3.0
     */
    public static Expression median(Object input) {
        return new MedianExpression(wrap(input));
    }

    /**
     * Returns the remainder of the first number divided by the second. Accepts two argument expressions.
     *
     * @param dividend the dividend
     * @param divisor  the divisor
     * @return the new expression
     * @aggregation.expression $mod
     */
    public static Expression mod(Object dividend, Object divisor) {
        return new MathExpression("$mod", wrap(List.of(dividend, divisor)));
    }

    /**
     * Multiplies numbers together and returns the result. Pass the arguments to $multiply in an array.
     *
     * @param first      the first value to multiply
     * @param additional any additional values
     * @return the new expression
     * @aggregation.expression $multiply
     */
    public static Expression multiply(Object first, Object... additional) {
        return new MathExpression("$multiply", wrap(first, additional));
    }

    /**
     * Returns an array of scalar values that correspond to specified percentile values.
     *
     * @param input       the input field or expression
     * @param percentiles the percentiles to compute
     * @return the new expression
     * @aggregation.expression $percentile
     * @mongodb.server.release 7.0
     * @since 3.0
     *
     */
    public static Expression percentile(Object input, List<Object> percentiles) {
        return new PercentileExpression(List.of(wrap(input)), wrap(percentiles.toArray(new Object[0])));
    }

    /**
     * Returns an array of scalar values that correspond to specified percentile values.
     *
     * @param inputs      the input fields or expressions
     * @param percentiles the percentiles to compute
     * @return the new expression
     * @aggregation.expression $percentile
     * @mongodb.server.release 7.0
     * @since 3.0
     *
     */
    public static Expression percentile(List<Object> inputs, List<Object> percentiles) {
        return new PercentileExpression(wrap(inputs), wrap(percentiles));
    }

    /**
     * Raises a number to the specified exponent.
     *
     * @param number   the base name
     * @param exponent the exponent
     * @return the new expression
     * @aggregation.expression $pow
     */
    public static Expression pow(Object number, Object exponent) {
        return new MathExpression("$pow", wrap(List.of(number, exponent)));
    }

    /**
     * Rounds a number to a whole integer or to a specified decimal place.
     *
     * @param number the value
     * @param place  the place to round to
     * @return the new expression
     * @aggregation.expression $round
     */
    public static Expression round(Object number, Object place) {
        return new MathExpression("$round", wrap(asList(number, place)));
    }

    /**
     * Calculates the square root.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $sqrt
     */
    public static Expression sqrt(Object value) {
        return new MathExpression("$sqrt", wrap(value));
    }

    /**
     * Returns the result of subtracting the second value from the first. If the two values are numbers, return the difference. If the two
     * values are dates, return the difference in milliseconds. If the two values are a date and a number in milliseconds, return the
     * resulting date. Accepts two argument expressions. If the two values are a date and a number, specify the date argument first as it
     * is not meaningful to subtract a date from a number.
     *
     * @param minuend    the number to subtract from
     * @param subtrahend the number to subtract
     * @return the new expression
     * @aggregation.expression $subtract
     */
    public static Expression subtract(Object minuend, Object subtrahend) {
        return new MathExpression("$subtract", wrap(List.of(minuend, subtrahend)));
    }

    /**
     * Truncates a number to a whole integer or to a specified decimal place.
     * <p>
     * NOTE: Prior to 4.2, the place value wasn't available. Pass null if your server is older than 4.2.
     *
     * @param number the value
     * @return the new expression
     * @aggregation.expression $trunc
     */
    public static Expression trunc(Object number) {
        return new MathExpression("$trunc", wrap(number));
    }

    /**
     * Truncates a number to a whole integer or to a specified decimal place.
     * <p>
     * NOTE: Prior to 4.2, the place value wasn't available. Use {@link #trunc(Object)} if your server is older than 4.2.
     *
     * @param number the value
     * @param place  the place to trunc to.
     * @return the new expression
     * @aggregation.expression $trunc
     * @see #trunc(Object)
     */
    public static Expression trunc(Object number, Object place) {
        return new MathExpression("$trunc", wrap(List.of(number, place)));
    }
}

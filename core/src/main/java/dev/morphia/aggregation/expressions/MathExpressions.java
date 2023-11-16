package dev.morphia.aggregation.expressions;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.lang.Nullable;

import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.MathExpression;
import dev.morphia.aggregation.expressions.impls.MedianExpression;

import static dev.morphia.aggregation.expressions.Expressions.value;
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
    public static Expression abs(Expression value) {
        return new MathExpression("$abs", value);
    }

    /**
     * Adds numbers together or adds numbers and a date. If one of the arguments is a date, $add treats the other arguments as
     * milliseconds to add to the date.
     *
     * @param first      the first expression to sum
     * @param additional any subsequent expressions to include in the sum
     * @return the new expression
     * @aggregation.expression $add
     */
    public static Expression add(Expression first, Expression... additional) {
        List<Expression> expressions = new ArrayList<>();
        expressions.add(first);
        expressions.addAll(asList(additional));
        return new MathExpression("$add", expressions);
    }

    /**
     * Returns the result of a bitwise and operation on an array of int or long values.
     *
     * @param first  the first expression to use
     * @param second the second expression to use
     * @return the new expression
     * @aggregation.expression $bitAnd
     * @since 3.0
     */
    public static Expression bitAnd(Expression first, Expression second) {
        return new MathExpression("$bitAnd", List.of(first, second));
    }

    /**
     * Returns the result of a bitwise not operation on a single int or long value.
     *
     * @param expression the expression to use
     * @return the new expression
     * @aggregation.expression $bitNot
     * @since 3.0
     */
    public static Expression bitNot(Expression expression) {
        return new MathExpression("$bitNot", expression);
    }

    /**
     * Returns the result of a bitwise or operation on an array of int or long values.
     *
     * @param first  the first expression to use
     * @param second the second expression to use
     * @return the new expression
     * @aggregation.expression $bitOr
     * @since 3.0
     */
    public static Expression bitOr(Expression first, Expression second) {
        return new MathExpression("$bitOr", List.of(first, second));
    }

    /**
     * Returns the result of a bitwise xor operation on an array of int xor long values.
     *
     * @param first  the first expression to use
     * @param second the second expression to use
     * @return the new expression
     * @aggregation.expression $bitXor
     * @since 3.0
     */
    public static Expression bitXor(Expression first, Expression second) {
        return new MathExpression("$bitXor", List.of(first, second));
    }

    /**
     * Returns the smallest integer greater than or equal to the specified number.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $ceil
     */
    public static Expression ceil(Expression value) {
        return new MathExpression("$ceil", value);
    }

    /**
     * Returns the result of dividing the first number by the second. Accepts two argument expressions.
     *
     * @param numerator the numerator
     * @param divisor   the divisor
     * @return the new expression
     * @aggregation.expression $divide
     */
    public static Expression divide(Expression numerator, Expression divisor) {
        return new MathExpression("$divide", List.of(numerator, divisor));
    }

    /**
     * Raises e to the specified exponent.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $exp
     */
    public static Expression exp(Expression value) {
        return new MathExpression("$exp", value);
    }

    /**
     * Returns the largest integer less than or equal to the specified number.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $floor
     */
    public static Expression floor(Expression value) {
        return new MathExpression("$floor", value);
    }

    /**
     * Calculates the natural log of a number.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $ln
     */
    public static Expression ln(Expression value) {
        return new MathExpression("$ln", value);
    }

    /**
     * Calculates the log of a number in the specified base.
     *
     * @param number the number to log
     * @param base   the base to use
     * @return the new expression
     * @aggregation.expression $log
     */
    public static Expression log(Expression number, Expression base) {
        return new MathExpression("$log", List.of(number, base));
    }

    /**
     * Calculates the log base 10 of a number.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $log10
     */
    public static Expression log10(Expression value) {
        return new MathExpression("$log10", value);
    }

    /**
     * Returns an approximation of the median, the 50th percentile, as a scalar value.
     *
     * @param input the input
     * @return the new expression
     * @aggregation.expression $median
     * @since 3.0
     */
    public static Expression median(Expression input) {
        return new MedianExpression(input);
    }

    /**
     * Returns the remainder of the first number divided by the second. Accepts two argument expressions.
     *
     * @param dividend the dividend
     * @param divisor  the divisor
     * @return the new expression
     * @aggregation.expression $mod
     */
    public static Expression mod(Expression dividend, Expression divisor) {
        return new MathExpression("$mod", List.of(dividend, divisor));
    }

    /**
     * Multiplies numbers together and returns the result. Pass the arguments to $multiply in an array.
     *
     * @param first      the first expression to add
     * @param additional any additional expressions
     * @return the new expression
     * @aggregation.expression $multiply
     */
    public static Expression multiply(Expression first, Expression... additional) {
        List<Expression> expressions = new ArrayList<>(List.of(first));
        expressions.addAll(asList(additional));
        return new MathExpression("$multiply", expressions);
    }

    /**
     * Raises a number to the specified exponent.
     *
     * @param number   the base name
     * @param exponent the exponent
     * @return the new expression
     * @aggregation.expression $pow
     */
    public static Expression pow(Expression number, Expression exponent) {
        return new MathExpression("$pow", List.of(number, exponent));
    }

    /**
     * Rounds a number to to a whole integer or to a specified decimal place.
     *
     * @param number the value
     * @param place  the place to round to
     * @return the new expression
     * @aggregation.expression $round
     */
    public static Expression round(Expression number, Expression place) {
        return new MathExpression("$round", asList(number, place));
    }

    /**
     * Calculates the square root.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $sqrt
     */
    public static Expression sqrt(Expression value) {
        return new MathExpression("$sqrt", value);
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
    public static Expression subtract(Expression minuend, Expression subtrahend) {
        return new MathExpression("$subtract", List.of(minuend, subtrahend));
    }

    /**
     * Truncates a number to a whole integer or to a specified decimal place.
     * <p>
     * NOTE: Prior to 4.2, the place value wasn't available. Pass null if your server is older than 4.2.
     *
     * @param number the value
     * @param place  the place to trunc to. may be null.
     * @return the new expression
     * @aggregation.expression $trunc
     */
    public static Expression trunc(Expression number, @Nullable Expression place) {
        ArrayList<Expression> params = new ArrayList<>();
        params.add(number);
        if (place != null) {
            params.add(place);
        }
        return new MathExpression("$trunc", params);
    }
}

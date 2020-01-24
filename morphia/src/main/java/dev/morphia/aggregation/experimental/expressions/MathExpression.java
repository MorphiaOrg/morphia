package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.codecs.ExpressionCodec;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Base class for the math expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#arithmetic-expression-operators Arithmetic Expressions
 * @since 2.0
 */
public class MathExpression extends Expression {
    private final List<Expression> operands;

    protected MathExpression(final String operation, final List<Expression> operands) {
        super(operation);
        this.operands = operands;
    }

    protected MathExpression(final String operation, final Expression operand) {
        super(operation);
        this.operands = List.of(operand);
    }

    /**
     * Adds numbers together or adds numbers and a date. If one of the arguments is a date, $add treats the other arguments as
     * milliseconds to add to the date.
     *
     * @param first      the first expression to sum
     * @param additional any subsequent expressions to include in the sum
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/add $add
     */
    public static MathExpression add(final Expression first, final Expression... additional) {
        List<Expression> expressions = new ArrayList<>();
        expressions.add(first);
        expressions.addAll(asList(additional));
        return new MathExpression("$add", expressions);
    }

    /**
     * Returns the result of dividing the first number by the second. Accepts two argument expressions.
     *
     * @param numerator the numerator
     * @param divisor   the divisor
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/divide $divide
     */
    public static Expression divide(final Expression numerator, final Expression divisor) {
        return new MathExpression("$divide", List.of(numerator, divisor));
    }

    /**
     * Multiplies numbers together and returns the result. Pass the arguments to $multiply in an array.
     *
     * @param first      the first expression to add
     * @param additional any additional expressions
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/multiply $multiply
     */
    public static Expression multiply(final Expression first, final Expression... additional) {
        List<Expression> expressions = new ArrayList<>(asList(first));
        expressions.addAll(asList(additional));
        return new MathExpression("$multiply", expressions);
    }

    /**
     * Returns the absolute value of a number.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/abs $abs
     */
    public static Expression abs(final Expression value) {
        return new MathExpression("$abs", value);
    }

    /**
     * Returns the smallest integer greater than or equal to the specified number.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/ceil $ceil
     */
    public static Expression ceil(final Expression value) {
        return new MathExpression("$ceil", value);
    }


    /**
     * Raises e to the specified exponent.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/exp $exp
     */
    public static Expression exp(final Expression value) {
        return new MathExpression("$exp", value);
    }

    /**
     * Returns the largest integer less than or equal to the specified number.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/floor $floor
     */
    public static Expression floor(final Expression value) {
        return new MathExpression("$floor", value);
    }

    /**
     * Calculates the natural log of a number.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/ln $ln
     */
    public static Expression ln(final Expression value) {
        return new MathExpression("$ln", value);
    }

    /**
     * Calculates the log of a number in the specified base.
     *
     * @param number the number to log
     * @param base   the base to use
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/log $log
     */
    public static Expression log(final Expression number, final Expression base) {
        return new MathExpression("$log", List.of(number, base));
    }

    /**
     * Calculates the log base 10 of a number.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/log10 $log10
     */
    public static Expression log10(final Expression value) {
        return new MathExpression("$log10", value);
    }

    /**
     * Returns the remainder of the first number divided by the second. Accepts two argument expressions.
     *
     * @param dividend the dividend
     * @param divisor  the divisor
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/mod $mod
     */
    public static Expression mod(final Expression dividend, final Expression divisor) {
        return new MathExpression("$mod", List.of(dividend, divisor));
    }

    /**
     * Raises a number to the specified exponent.
     *
     * @param number   the base name
     * @param exponent the exponent
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/pow $pow
     */
    public static Expression pow(final Expression number, final Expression exponent) {
        return new MathExpression("$pow", List.of(number, exponent));
    }

    /**
     * Rounds a number to to a whole integer or to a specified decimal place.
     *
     * @param number the value
     * @param place  the place to round to
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/round $round
     */
    public static Expression round(final Expression number, final Expression place) {
        return new MathExpression("$round", List.of(number, place));
    }

    /**
     * Calculates the square root.
     *
     * @param value the value
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/sqrt $sqrt
     */
    public static Expression sqrt(final Expression value) {
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
     * @mongodb.driver.manual reference/operator/aggregation/subtract $subtract
     */
    public static Expression subtract(final Expression minuend, final Expression subtrahend) {
        return new MathExpression("$subtract", List.of(minuend, subtrahend));
    }

    /**
     * Truncates a number to a whole integer or to a specified decimal place.
     *
     * @param number the value
     * @param place  the place to trunc to
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/trunc $trunc
     */
    public static Expression trunc(final Expression number, final Expression place) {
        return new MathExpression("$trunc", List.of(number, place));
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName(getOperation());
        if (operands.size() > 1) {
            writer.writeStartArray();
        }
        for (final Expression operand : operands) {
            ExpressionCodec.writeUnnamedExpression(mapper, writer, operand, encoderContext);
        }
        if (operands.size() > 1) {
            writer.writeEndArray();
        }
        writer.writeEndDocument();
    }
}

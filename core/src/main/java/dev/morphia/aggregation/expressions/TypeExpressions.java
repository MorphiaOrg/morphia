package dev.morphia.aggregation.expressions;

import dev.morphia.aggregation.expressions.impls.ConvertExpression;
import dev.morphia.aggregation.expressions.impls.ConvertType;
import dev.morphia.aggregation.expressions.impls.Expression;

import static dev.morphia.aggregation.expressions.Expressions.wrap;

/**
 * Defines helper methods for the type expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#type-expression-operators Type Expressions
 * @since 2.0
 */
public final class TypeExpressions {
    private TypeExpressions() {
    }

    /**
     * Converts a value to a specified type.
     *
     * @param input the value to process
     * @param to    an expression giving the target type
     * @return the new expression
     * @aggregation.expression $convert
     */
    public static ConvertExpression convert(Object input, ConvertType to) {
        return new ConvertExpression(wrap(input), to);
    }

    /**
     * Checks if the specified expression resolves to one of the <a href="https://docs.mongodb.com/manual/reference/bson-types/#bson-types">
     * numeric BSON types.</a>
     *
     * @param input the value to check
     * @return the new expression
     * @aggregation.expression $isNumber
     * @mongodb.server.release 4.4
     * @since 2.1
     */
    public static Expression isNumber(Object input) {
        return new Expression("$isNumber", wrap(input));
    }

    /**
     * Converts value to a boolean.
     *
     * @param input the value to process
     * @return the new expression
     * @aggregation.expression $toBool
     */
    public static Expression toBool(Object input) {
        return new Expression("$toBool", wrap(input));
    }

    /**
     * Converts value to a Decimal128.
     *
     * @param input the value to process
     * @return the new expression
     * @aggregation.expression $toDecimal
     */
    public static Expression toDecimal(Object input) {
        return new Expression("$toDecimal", wrap(input));
    }

    /**
     * Converts value to a double.
     *
     * @param input the value to process
     * @return the new expression
     * @aggregation.expression $toDouble
     */
    public static Expression toDouble(Object input) {
        return new Expression("$toDouble", wrap(input));
    }

    /**
     * Converts value to an integer.
     *
     * @param input the value to process
     * @return the new expression
     * @aggregation.expression $toInt
     */
    public static Expression toInt(Object input) {
        return new Expression("$toInt", wrap(input));
    }

    /**
     * Converts value to a long.
     *
     * @param input the value to process
     * @return the new expression
     * @aggregation.expression $toLong
     */
    public static Expression toLong(Object input) {
        return new Expression("$toLong", wrap(input));
    }

    /**
     * Converts value to an ObjectId.
     *
     * @param input the value to process
     * @return the new expression
     * @aggregation.expression $toObjectId
     */
    public static Expression toObjectId(Object input) {
        return new Expression("$toObjectId", wrap(input));
    }

    /**
     * Converts value to a string.
     *
     * @param input the value to process
     * @return the new expression
     * @aggregation.expression $toString
     */
    public static Expression toString(Object input) {
        return StringExpressions.toString(input);
    }

    /**
     * Return the BSON data type of the field.
     *
     * @param input the value to process
     * @return the new expression
     * @aggregation.expression $type
     */
    public static Expression type(Object input) {
        return new Expression("$type", wrap(input));
    }
}

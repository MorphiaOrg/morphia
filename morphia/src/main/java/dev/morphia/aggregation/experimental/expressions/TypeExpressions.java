package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.aggregation.experimental.expressions.impls.ConvertExpression;
import dev.morphia.aggregation.experimental.expressions.impls.ConvertType;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;

/**
 * Defines helper methods for the type expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#type-expression-operators Type Expressions
 * @since 2.0
 */
public final class TypeExpressions {
    protected TypeExpressions() {
    }

    /**
     * Converts a value to a specified type.
     *
     * @param input the value to process
     * @param to    an expression giving the target type
     * @return the new expression
     * @aggregation.expression $convert
     */
    public static Expression convert(Expression input, ConvertType to) {
        return new ConvertExpression(input, to);
    }

    /**
     * Checks if the specified expression resolves to one of the <a hre="https://docs.mongodb.com/manual/reference/bson-types/#bson-types">
     * numeric BSON types.</a>
     *
     * @param input the value to check
     * @return the new expression
     * @aggregation.expression $isNumber
     * @since 2.1
     */
    public static Expression isNumber(Expression input) {
        return new Expression("$isNumber", input);
    }

    /**
     * Converts value to a boolean.
     *
     * @param input the value to process
     * @return the new expression
     * @aggregation.expression $toBool
     */
    public static Expression toBool(Expression input) {
        return new Expression("$toBool", input);
    }

    /**
     * Converts value to a Date.
     *
     * @param input the value to process
     * @return the new expression
     * @aggregation.expression $toDate
     */
    public static Expression toDate(Expression input) {
        return new Expression("$toDate", input);
    }

    /**
     * Converts value to a Decimal128.
     *
     * @param input the value to process
     * @return the new expression
     * @aggregation.expression $toDecimal
     */
    public static Expression toDecimal(Expression input) {
        return new Expression("$toDecimal", input);
    }

    /**
     * Converts value to a double.
     *
     * @param input the value to process
     * @return the new expression
     * @aggregation.expression $toDouble
     */
    public static Expression toDouble(Expression input) {
        return new Expression("$toDouble", input);
    }

    /**
     * Converts value to an integer.
     *
     * @param input the value to process
     * @return the new expression
     * @aggregation.expression $toInt
     */
    public static Expression toInt(Expression input) {
        return new Expression("$toInt", input);
    }

    /**
     * Converts value to a long.
     *
     * @param input the value to process
     * @return the new expression
     * @aggregation.expression $toLong
     */
    public static Expression toLong(Expression input) {
        return new Expression("$toLong", input);
    }

    /**
     * Converts value to an ObjectId.
     *
     * @param input the value to process
     * @return the new expression
     * @aggregation.expression $toObjectId
     */
    public static Expression toObjectId(Expression input) {
        return new Expression("$toObjectId", input);
    }

    /**
     * Converts value to a string.
     *
     * @param input the value to process
     * @return the new expression
     * @aggregation.expression $toString
     */
    public static Expression toString(Expression input) {
        return StringExpressions.toString(input);
    }

    /**
     * Return the BSON data type of the field.
     *
     * @param input the value to process
     * @return the new expression
     * @aggregation.expression $type
     */
    public static Expression type(Expression input) {
        return new Expression("$type", input);
    }
}

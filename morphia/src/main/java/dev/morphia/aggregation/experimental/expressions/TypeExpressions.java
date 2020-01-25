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
     * @mongodb.driver.manual reference/operator/aggregation/convert $convert
     */
    public static Expression convert(final Expression input, final ConvertType to) {
        return new ConvertExpression(input, to);
    }

    /**
     * Converts value to a boolean.
     *
     * @param input the value to process
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/toBool $toBool
     */
    public static Expression toBool(final Expression input) {
        return new Expression("$toBool", input);
    }

    /**
     * Converts value to a Date.
     *
     * @param input the value to process
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/toDate $toDate
     */
    public static Expression toDate(final Expression input) {
        return new Expression("$toDate", input);
    }

    /**
     * Converts value to a Decimal128.
     *
     * @param input the value to process
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/toDecimal $toDecimal
     */
    public static Expression toDecimal(final Expression input) {
        return new Expression("$toDecimal", input);
    }

    /**
     * Converts value to a double.
     *
     * @param input the value to process
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/toDouble $toDouble
     */
    public static Expression toDouble(final Expression input) {
        return new Expression("$toDouble", input);
    }

    /**
     * Converts value to an integer.
     *
     * @param input the value to process
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/toInt $toInt
     */
    public static Expression toInt(final Expression input) {
        return new Expression("$toInt", input);
    }

    /**
     * Converts value to a long.
     *
     * @param input the value to process
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/toLong $toLong
     */
    public static Expression toLong(final Expression input) {
        return new Expression("$toLong", input);
    }

    /**
     * Converts value to an ObjectId.
     *
     * @param input the value to process
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/toObjectId $toObjectId
     */
    public static Expression toObjectId(final Expression input) {
        return new Expression("$toObjectId", input);
    }

    /**
     * Converts value to a string.
     *
     * @param input the value to process
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/toString $toString
     */
    public static Expression toString(final Expression input) {
        return StringExpressions.toString(input);
    }

    /**
     * Return the BSON data type of the field.
     *
     * @param input the value to process
     * @return the new expression
     * @mongodb.driver.manual reference/operator/aggregation/type $type
     */
    public static Expression type(final Expression input) {
        return new Expression("$type", input);
    }
}

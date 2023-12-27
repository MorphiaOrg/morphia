package dev.morphia.aggregation.expressions;

import java.util.List;

import dev.morphia.aggregation.expressions.impls.Expression;

import static dev.morphia.aggregation.expressions.Expressions.wrap;

/**
 * Defines helper methods for the trigonometry expressions
 *
 * @mongodb.driver.manual reference/operator/aggregation/#trigonometry-expression-operators Trigonometry Expressions
 */
public final class TrigonometryExpressions {
    private TrigonometryExpressions() {
    }

    /**
     * Returns the inverse cosine (arc cosine) of a value in radians.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $acos
     */
    public static Expression acos(Object value) {
        return new Expression("$acos", wrap(value));
    }

    /**
     * Returns the inverse hyperbolic cosine (hyperbolic arc cosine) of a value in radians.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $acosh
     */
    public static Expression acosh(Object value) {
        return new Expression("$acosh", wrap(value));
    }

    /**
     * Returns the inverse sin (arc sine) of a value in radians.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $asin
     */
    public static Expression asin(Object value) {
        return new Expression("$asin", wrap(value));
    }

    /**
     * Returns the inverse hyperbolic sine (hyperbolic arc sine) of a value in radians.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $asinh
     */
    public static Expression asinh(Object value) {
        return new Expression("$asinh", wrap(value));
    }

    /**
     * Returns the inverse tangent (arc tangent) of a value in radians.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $atan
     */
    public static Expression atan(Object value) {
        return new Expression("$atan", wrap(value));
    }

    /**
     * Returns the inverse tangent (arc tangent) of y / x in radians, where y and x are the first and second values passed to the
     * expression respectively.
     *
     * @param yValue the y value
     * @param xValue the x value
     * @return the new expression
     * @aggregation.expression $atan2
     */
    public static Expression atan2(Object yValue, Object xValue) {
        return new Expression("$atan2", wrap(List.of(yValue, xValue)));
    }

    /**
     * Returns the inverse hyperbolic tangent (hyperbolic arc tangent) of a value in radians.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $atanh
     */
    public static Expression atanh(Object value) {
        return new Expression("$atanh", wrap(value));
    }

    /**
     * Returns the cosine of a value that is measured in radians.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $cos
     */
    public static Expression cos(Object value) {
        return new Expression("$cos", wrap(value));
    }

    /**
     * Returns the hyperbolic cosine of a value that is measured in radians.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $cosh
     * @since 2.2
     */
    public static Expression cosh(Object value) {
        return new Expression("$cosh", wrap(value));
    }

    /**
     * Converts a value from degrees to radians.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $degreesToRadians
     */
    public static Expression degreesToRadians(Object value) {
        return new Expression("$degreesToRadians", wrap(value));
    }

    /**
     * Converts a value from radians to degrees.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $radiansToDegrees
     */
    public static Expression radiansToDegrees(Object value) {
        return new Expression("$radiansToDegrees", wrap(value));
    }

    /**
     * Returns the sine of a value that is measured in radians.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $sin
     */
    public static Expression sin(Object value) {
        return new Expression("$sin", wrap(value));
    }

    /**
     * Returns the hyperbolic sine of a value that is measured in radians.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $sinh
     * @since 2.2
     */
    public static Expression sinh(Object value) {
        return new Expression("$sinh", wrap(value));
    }

    /**
     * Returns the tangent of a value that is measured in radians.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $tan
     */
    public static Expression tan(Object value) {
        return new Expression("$tan", wrap(value));
    }

    /**
     * Returns the hyperbolic tangent of a value that is measured in radians.
     *
     * @param value the value
     * @return the new expression
     * @aggregation.expression $tanh
     * @since 2.2
     */
    public static Expression tanh(Object value) {
        return new Expression("$tanh", wrap(value));
    }

}

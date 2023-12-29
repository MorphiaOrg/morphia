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
     * @mongodb.server.release 4.2
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
     * @mongodb.server.release 4.2
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
     * @mongodb.server.release 4.2
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
     * @mongodb.server.release 4.2
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
     * @mongodb.server.release 4.2
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
     * @mongodb.server.release 4.2
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
     * @mongodb.server.release 4.2
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
     * @mongodb.server.release 4.2
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
     * @mongodb.server.release 4.2
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
     * @mongodb.server.release 4.2
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
     * @mongodb.server.release 4.2
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
     * @mongodb.server.release 4.2
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
     * @mongodb.server.release 4.2
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
     * @mongodb.server.release 4.2
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
     * @mongodb.server.release 4.2
     * @since 2.2
     */
    public static Expression tanh(Object value) {
        return new Expression("$tanh", wrap(value));
    }

}
